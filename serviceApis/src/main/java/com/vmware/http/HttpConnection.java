package com.vmware.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.cookie.CookieFileStore;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.ExceptionChecker;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.OverwritableSet;
import com.vmware.http.request.RequestBodyFactory;
import com.vmware.http.request.RequestBodyHandling;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.ssl.WorkflowCertificateManager;
import com.vmware.utils.IOUtils;
import com.vmware.utils.ThreadUtils;
import com.vmware.utils.input.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.vmware.http.HttpMethodType.GET;
import static com.vmware.http.HttpMethodType.POST;
import static com.vmware.http.HttpMethodType.PUT;
import static com.vmware.http.HttpMethodType.DELETE;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;

/**
 * Using Java's HttpURLConnection instead of Apache HttpClient to cut down on jar size
 */
public class HttpConnection {

    private static Logger log = LoggerFactory.getLogger(HttpConnection.class.getName());
    private static int CONNECTION_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(25, TimeUnit.SECONDS);
    private static final int MAX_REQUEST_RETRIES = 3;

    private final CookieFileStore cookieFileStore;
    private WorkflowCertificateManager workflowCertificateManager = null;
    private Gson gson;
    private RequestBodyHandling requestBodyHandling;
    private Set<RequestParam> statefulParams = new OverwritableSet<RequestParam>();
    private HttpURLConnection activeConnection;
    private boolean useSessionCookies;

    public HttpConnection(RequestBodyHandling requestBodyHandling) throws IOException {
        this.requestBodyHandling = requestBodyHandling;
        this.gson = new ConfiguredGsonBuilder().build();

        String homeFolder = System.getProperty("user.home");
        cookieFileStore = new CookieFileStore(homeFolder);
        workflowCertificateManager = new WorkflowCertificateManager(homeFolder + "/.workflowTool.keystore");
    }

    public void updateServerTimeZone(TimeZone serverTimezone, String serverDateFormat) {
        this.gson = new ConfiguredGsonBuilder(serverTimezone, serverDateFormat).build();
    }

    public void setupBasicAuthHeader(final UsernamePasswordCredentials credentials) {
        String basicCredentials = DatatypeConverter.printBase64Binary(credentials.toString().getBytes());
        RequestHeader authorizationHeader = new RequestHeader("Authorization", "Basic " + basicCredentials);
        statefulParams.add(authorizationHeader);
    }

    public void addStatefulParams(List<? extends RequestParam> params) {
        statefulParams.addAll(params);
    }

    public void clearStatefulParams() {
        statefulParams.clear();
    }

    public <T> T get(String url, Class<T> responseConversionClass, List<RequestParam> params) throws IOException, URISyntaxException {
        return get(url, responseConversionClass, params.toArray(new RequestParam[params.size()]));
    }

    public <T> T get(String url, Class<T> responseConversionClass, RequestParam... params)
            throws IOException, URISyntaxException {
        setupConnection(url, GET, params);
        return handleServerResponse(responseConversionClass, GET, params);
    }

    public <T> T put(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, PUT, params);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass, PUT, params);
    }

    public <T> T post(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, POST, params);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass, POST, params);
    }

    public <T> T put(String url, Object requestObject, RequestParam... params)
            throws URISyntaxException, IOException, IllegalAccessException {
        return put(url, null, requestObject, params);
    }

    public <T> T post(String url, Object requestObject, RequestParam... params)
            throws URISyntaxException, IOException, IllegalAccessException {
        return post(url, null, requestObject, params);
    }

    public <T> T delete(String url, RequestParam... params)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, DELETE, params);
        return handleServerResponse(null, DELETE, params);
    }

    public boolean isUriTrusted(URI uri) throws IOException {
        return workflowCertificateManager.isUriTrusted(uri);
    }

    public void setRequestBodyHandling(final RequestBodyHandling requestBodyHandling) {
        this.requestBodyHandling = requestBodyHandling;
    }

    public boolean hasCookie(ApiAuthentication ApiAuthentication) {
        Cookie cookie = cookieFileStore.getCookieByName(ApiAuthentication.getCookieName());
        return cookie != null;
    }

    public void setRequestProperty(String name, String value) {
        activeConnection.setRequestProperty(name, value);
    }

    public OutputStream getOutputStream() throws IOException {
        return activeConnection.getOutputStream();
    }

    public RequestBodyHandling getRequestBodyHandling() {
        return requestBodyHandling;
    }

    public void setDoOutput(boolean value) {
        activeConnection.setDoOutput(value);
    }

    public void setUseSessionCookies(boolean useSessionCookies) {
        this.useSessionCookies = useSessionCookies;
    }

    public String toJson(Object value) {
        return gson.toJson(value);
    }

    private void setupConnection(String requestUrl, HttpMethodType methodType, RequestParam... statelessParams) throws IOException, URISyntaxException {
        Set<RequestParam> allParams = new OverwritableSet<RequestParam>(statefulParams);
        // add default application json header, can be overridden by stateless headers
        allParams.add(anAcceptHeader("application/json"));

        List<RequestParam> statelessParamsList = Arrays.asList(statelessParams);
        allParams.addAll(statelessParamsList);
        String fullUrl = UrlUtils.buildUrl(requestUrl, allParams);
        URI uri = new URI(fullUrl);
        log.debug("{}: {}", methodType.name(), uri.toString());

        activeConnection = (HttpURLConnection) uri.toURL().openConnection();
        activeConnection.setDoInput(true);
        activeConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        activeConnection.setReadTimeout(CONNECTION_TIMEOUT);
        activeConnection.setInstanceFollowRedirects(false);
        activeConnection.setRequestMethod(methodType.name());
        addRequestHeaders(allParams);
        addCookiesHeader(uri.getHost());
    }

    private void addRequestHeaders(Collection<? extends RequestParam> params) {
        for (RequestParam param : params) {
            if (!(param instanceof RequestHeader)) {
                continue;
            }
            RequestHeader header = (RequestHeader) param;
            log.debug("Adding request header {}:{}", header.getName(), header.getValue());
            activeConnection.setRequestProperty(header.getName(), header.getValue());
        }
    }

    private <T> T handleServerResponse(final Class<T> responseConversionClass, HttpMethodType methodTypes, RequestParam[] params) throws IOException {
        String responseText = getResponseText(0, methodTypes, params);
        activeConnection.disconnect();
        if (responseText.isEmpty() || responseConversionClass == null) {
            return null;
        } else {
            try {
                return gson.fromJson(responseText, responseConversionClass);
            } catch (JsonSyntaxException e) {
                // allow a parsing attempt as it could be a json string primitive
                if (responseConversionClass.equals(String.class)) {
                    return (T) responseText;
                } else {
                    log.error("Failed to parse response text\n{}", responseText);
                    throw e;
                }
            }
        }
    }

    private void addCookiesHeader(String host) {
        activeConnection.setRequestProperty("Cookie", cookieFileStore.toCookieRequestText(host, useSessionCookies));
    }

    private String getResponseText(int retryCount, HttpMethodType methodType, RequestParam... params) throws IOException {
        String responseText = "";
        try {
            responseText = parseResponseText();
            cookieFileStore.addCookiesFromResponse(activeConnection);
        } catch (SSLException e) {
            String urlText = activeConnection.getURL().toString();
            log.error("Ssl error for {} {}", activeConnection.getRequestMethod(), urlText);
            log.error("Error [{}]" ,e.getMessage());
            askIfSslCertShouldBeSaved();
            exitIfMaxRetriesReached(retryCount);

            ThreadUtils.sleep(2, TimeUnit.SECONDS);
            log.info("");
            log.info("Retrying request {} of {}", ++retryCount, MAX_REQUEST_RETRIES);
            reconnect(methodType, urlText, params);
            responseText = getResponseText(retryCount, methodType, params);
        } catch (UnknownHostException | SocketException e) {
            handleNetworkException(e);
        }
        return responseText;
    }

    private void reconnect(HttpMethodType methodType, String urlText, RequestParam[] params) throws IOException {
        activeConnection.disconnect();
        try {
            setupConnection(urlText, methodType, params);
        } catch (URISyntaxException use) {
            throw new IOException(use);
        }
    }

    private void askIfSslCertShouldBeSaved() throws IOException {
        URI uri;
        try {
            uri = activeConnection.getURL().toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        if (workflowCertificateManager == null || workflowCertificateManager.isUriTrusted(uri)) {
            log.info("Url {} is already trusted, no need to save cert to local keystore");
            return;
        }
        log.info("Host {} is not trusted, do you want to save the cert for this to the local workflow trust store {}",
                uri.getHost(), workflowCertificateManager.getKeyStore());
        log.warn("NB: ONLY save the certificate if you trust the host shown");
        String response = InputUtils.readValue("Save certificate? [Y/N] ");
        if ("Y".equalsIgnoreCase(response)) {
            workflowCertificateManager.saveCertForUri(uri);
        }
    }

    private void exitIfMaxRetriesReached(int retryCount) {
        if (retryCount >= MAX_REQUEST_RETRIES) {
            try {
                exitDueToSslExceptions();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void exitDueToSslExceptions() throws URISyntaxException {
        String url = activeConnection.getURL().toURI().toString();
        log.info("");
        log.info("Still getting ssl errors, can't proceed");
        if (url.contains("reviewboard")) {
            log.info("Sometimes there are one off ssl exceptions with the reviewboard api, try rerunning your workflow");
        }
        System.exit(1);
    }

    private void handleNetworkException(IOException e) {
        log.info("");
        log.error("Unknown host exception thrown: {}", e.getMessage());
        log.error("Are you connected to the corporate network?");
        log.error("Failed to access host {}", activeConnection.getURL().getHost());
        System.exit(1);
    }

    private String parseResponseText() throws IOException {
        String responseText;
        String currentUrl = activeConnection.getURL().toExternalForm();
        int responseCode = activeConnection.getResponseCode();
        if (ExceptionChecker.isStatusValid(responseCode)) {
            responseText = IOUtils.read(activeConnection.getInputStream());
        } else {
            responseText = IOUtils.read(activeConnection.getErrorStream());
        }
        log.trace("Response\n{}", responseText);
        ExceptionChecker.throwExceptionIfStatusIsNotValid(currentUrl, responseCode, responseText);
        return responseText;
    }

}
