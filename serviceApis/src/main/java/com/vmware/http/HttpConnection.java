package com.vmware.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.cookie.CookieFileStore;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.ExceptionChecker;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.body.RequestBodyFactory;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.ssl.WorkflowCertificateManager;
import com.vmware.util.IOUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.exception.RuntimeURISyntaxException;
import com.vmware.util.input.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.vmware.http.HttpMethodType.GET;
import static com.vmware.http.HttpMethodType.POST;
import static com.vmware.http.HttpMethodType.PUT;
import static com.vmware.http.HttpMethodType.DELETE;
import static com.vmware.http.request.RequestHeader.aBasicAuthHeader;
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
    private RequestParams requestParams;
    private HttpURLConnection activeConnection;
    private boolean useSessionCookies;

    public HttpConnection(RequestBodyHandling requestBodyHandling) {
        this(requestBodyHandling, null);
    }

    public HttpConnection(RequestBodyHandling requestBodyHandling, Map<String, String> customFieldNames) {
        this.requestBodyHandling = requestBodyHandling;
        this.gson = new ConfiguredGsonBuilder(customFieldNames).build();

        String homeFolder = System.getProperty("user.home");
        cookieFileStore = new CookieFileStore(homeFolder);
        workflowCertificateManager = new WorkflowCertificateManager(homeFolder + "/.workflowTool.keystore");
        requestParams = new RequestParams();
    }

    public void updateTimezoneAndFormat(TimeZone serverTimezone, String serverDateFormat) {
        this.gson = new ConfiguredGsonBuilder(serverTimezone, serverDateFormat).build();
    }

    public void setupBasicAuthHeader(final UsernamePasswordCredentials credentials) {
        requestParams.addStatefulParam(aBasicAuthHeader(credentials));
    }

    public void addStatefulParams(List<? extends RequestParam> params) {
        requestParams.addAllStatefulParams(params);
    }

    public void addStatefulParam(RequestParam requestParam) {
        requestParams.addStatefulParam(requestParam);
    }

    public void removeStatefulParam(String paramName) {
        requestParams.removeStatefulParam(paramName);
    }

    public void addStatefulParamsFromUrlFragment(String urlFragment) {
        requestParams.addStatefulParamsFromUrlFragment(urlFragment);
    }

    public void addCookie(Cookie cookie) {
        cookieFileStore.addCookieIfUseful(cookie);
    }

    public void resetParams() {
        requestParams.reset();
    }

    public <T> T get(String url, Class<T> responseConversionClass, List<RequestParam> params) {
        return get(url, responseConversionClass, params.toArray(new RequestParam[params.size()]));
    }

    public <T> T get(String url, Class<T> responseConversionClass, RequestParam... params) {
        setupConnection(url, GET, params);
        return handleServerResponse(responseConversionClass, GET, params);
    }

    public <T> T put(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params) {
        setupConnection(url, PUT, params);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass, PUT, params);
    }

    public <T> T post(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params) {
        setupConnection(url, POST, params);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass, POST, params);
    }

    public <T> T put(String url, Object requestObject, RequestParam... params) {
        return put(url, null, requestObject, params);
    }

    public <T> T post(String url, Object requestObject, RequestParam... params) {
        return post(url, null, requestObject, params);
    }

    public <T> T delete(String url, RequestParam... params) {
        return delete(url, null, params);
    }

    public <T> T delete(String url, Class<T> responseConversionClass, RequestParam... params) {
        setupConnection(url, DELETE, params);
        return handleServerResponse(responseConversionClass, DELETE, params);
    }

    public boolean isUriTrusted(URI uri) {
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

    public boolean containsRequestHeader(String name) {
        return requestParams.requestHeaders().stream().anyMatch(header -> header.getName().equalsIgnoreCase(name));
    }

    private void setupConnection(String requestUrl, HttpMethodType methodType, RequestParam... statelessParams) {
        requestParams.clearStatelessParams();
        // add default application json header, can be overridden by stateless headers
        requestParams.addStatelessParam(anAcceptHeader("application/json"));

        List<RequestParam> statelessParamsList = statelessParams != null ?
                new ArrayList<>(Arrays.asList(statelessParams)) : Collections.emptyList();
        statelessParamsList.removeIf(Objects::isNull);
        requestParams.addAllStatelessParams(statelessParamsList);
        String fullUrl = requestParams.buildUrl(requestUrl);
        URI uri = URI.create(fullUrl);
        log.debug("{}: {}", methodType.name(), uri.toString());

        try {
            activeConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        activeConnection.setDoInput(true);
        activeConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        activeConnection.setReadTimeout(CONNECTION_TIMEOUT);
        activeConnection.setInstanceFollowRedirects(false);
        try {
            activeConnection.setRequestMethod(methodType.name());
        } catch (ProtocolException e) {
            throw new RuntimeIOException(e);
        }
        addRequestHeaders();
        addCookiesHeader(uri.getHost());
    }

    private void addRequestHeaders() {
        for (RequestHeader header : requestParams.requestHeaders()) {
            log.debug("Adding request header {}:{}", header.getName(), header.getValue());
            activeConnection.setRequestProperty(header.getName(), header.getValue());
        }
    }

    private <T> T handleServerResponse(final Class<T> responseConversionClass, HttpMethodType methodTypes, RequestParam[] params) {
        String responseText = getResponseText(0, methodTypes, params);
        if (responseConversionClass == HttpResponse.class) {
            return (T) new HttpResponse(responseText, activeConnection.getHeaderFields());
        }
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
        String cookieHeaderValue = cookieFileStore.toCookieRequestText(host, useSessionCookies);
        log.trace("Cookie header {}", cookieHeaderValue);
        activeConnection.setRequestProperty("Cookie", cookieHeaderValue);
    }

    private String getResponseText(int retryCount, HttpMethodType methodType, RequestParam... params) {
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
        } catch (IOException ioe) {
            throw new RuntimeIOException(ioe);
        }
        return responseText;
    }

    private void reconnect(HttpMethodType methodType, String urlText, RequestParam[] params) {
        activeConnection.disconnect();
        setupConnection(urlText, methodType, params);
    }

    private void askIfSslCertShouldBeSaved() {
        URI uri;
        try {
            uri = activeConnection.getURL().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeURISyntaxException(e);
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
            exitDueToSslExceptions();
        }
    }

    private void exitDueToSslExceptions() {
        String url;
        try {
            url = activeConnection.getURL().toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeURISyntaxException(e);
        }
        log.info("");
        log.info("Still getting ssl errors, can't proceed");
        if (url.contains("reviewboard")) {
            log.info("Sometimes there are one off ssl exceptions with the reviewboard api, try rerunning your workflow");
        }
        System.exit(1);
    }

    private void handleNetworkException(IOException e) {
        throw new FatalException(e, "Unknown host exception thrown: " + e.getMessage()
                + "\nAre you connected to the corporate network?"
                + "\nFailed to access host " + activeConnection.getURL().getHost());
    }

    private String parseResponseText() throws IOException {
        String currentUrl = activeConnection.getURL().toExternalForm();
        int responseCode = activeConnection.getResponseCode();
        String responseText;
        if (ExceptionChecker.isStatusValid(responseCode) || activeConnection.getErrorStream() == null) {
            responseText = IOUtils.read(activeConnection.getInputStream());
        } else {
            responseText = IOUtils.read(activeConnection.getErrorStream());
        }
        log.trace("Response\n{}", responseText);
        ExceptionChecker.throwExceptionIfStatusIsNotValid(currentUrl, responseCode, responseText);
        return responseText;
    }

}
