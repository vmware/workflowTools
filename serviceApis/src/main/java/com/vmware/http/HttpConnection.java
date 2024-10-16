package com.vmware.http;

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
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.cookie.CookieFileStore;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.ExceptionChecker;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.body.RequestBodyFactory;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.http.ssl.WorkflowCertificateManager;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.exception.RuntimeURISyntaxException;
import com.vmware.util.input.InputUtils;

import com.vmware.util.logging.Padder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vmware.http.HttpMethodType.DELETE;
import static com.vmware.http.HttpMethodType.GET;
import static com.vmware.http.HttpMethodType.PATCH;
import static com.vmware.http.HttpMethodType.POST;
import static com.vmware.http.HttpMethodType.PUT;
import static com.vmware.http.request.RequestHeader.aBasicAuthHeader;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;

/**
 * Using Java's HttpURLConnection instead of Apache HttpClient to cut down on jar size
 */
public class HttpConnection {

    private static final Logger log = LoggerFactory.getLogger(HttpConnection.class.getName());
    private static final int CONNECTION_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(25, TimeUnit.SECONDS);
    public static final int MAX_REQUEST_RETRIES = 3;

    public static boolean alwaysDisableHostnameVerification;

    private final CookieFileStore cookieFileStore;
    private WorkflowCertificateManager workflowCertificateManager = null;
    private Gson gson;
    private RequestBodyHandling requestBodyHandling;
    private final RequestParams requestParams;
    private HttpURLConnection activeConnection;
    private boolean useSessionCookies;
    private boolean disableHostnameVerification;

    public HttpConnection(RequestBodyHandling requestBodyHandling) {
        this(requestBodyHandling, new ConfiguredGsonBuilder().build());
    }

    public HttpConnection(RequestBodyHandling requestBodyHandling, Gson gson) {
        this.requestBodyHandling = requestBodyHandling;
        this.gson = gson;

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

    public CookieFileStore getCookieFileStore() {
        return cookieFileStore;
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

    public void removeCookie(String cookieName) {
        cookieFileStore.removeCookie(cookieName);
    }

    public void resetParams() {
        requestParams.reset();
    }

    public <T> T executeApiRequest(HttpMethodType methodType, String url, Class<T> responseConversionClass, Object requestObject, RequestParam[] params) {
        Padder requestPadder = new Padder("{} {}", methodType.name(), url);
        requestPadder.debugTitle();
        setupConnection(url, methodType, params);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        T response = handleServerResponse(url, responseConversionClass, methodType, params);
        requestPadder.debugTitle();;
        return response;
    }


    public <T> T get(String url, Class<T> responseConversionClass, RequestParam... params) {
        return executeApiRequest(GET, url, responseConversionClass, null, params);
    }

    public <T> T patch(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params) {
        return executeApiRequest(PATCH, url, responseConversionClass, requestObject, params);
    }

    public <T> T put(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params) {
        return executeApiRequest(PUT, url, responseConversionClass, requestObject, params);
    }

    public <T> T post(String url, Class<T> responseConversionClass, Object requestObject, RequestParam... params) {
        return executeApiRequest(POST, url, responseConversionClass, requestObject, params);
    }

    public <T> T put(String url, Object requestObject, RequestParam... params) {
        return put(url, null, requestObject, params);
    }

    public <T> T post(String url, Object requestObject, RequestParam... params) {
        return post(url, null, requestObject, params);
    }

    public <T> T delete(String url, RequestParam... params) {
        return delete(url, null, null, params);
    }

    public <T> T delete(String url, Object requestObject, Class<T> responseConversionClass, RequestParam... params) {
        return executeApiRequest(DELETE, url, responseConversionClass, requestObject, params);
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

    public void setDisableHostnameVerification(boolean disableHostnameVerification) {
        this.disableHostnameVerification = disableHostnameVerification;
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
        log.debug("{}: {}", methodType.name(), uri);

        try {
            activeConnection = (HttpURLConnection) uri.toURL().openConnection();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        activeConnection.setDoInput(true);
        activeConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        activeConnection.setReadTimeout(CONNECTION_TIMEOUT);
        activeConnection.setInstanceFollowRedirects(false);
        if (activeConnection instanceof HttpsURLConnection && (disableHostnameVerification || alwaysDisableHostnameVerification)) {
            // allow all host names
            ((HttpsURLConnection) activeConnection).setHostnameVerifier((s, sslSession) -> true);
        }
        try {
            if (methodType == PATCH) {
                activeConnection.setRequestMethod(POST.name());
                activeConnection.setRequestProperty("X-HTTP-Method-Override", PATCH.name());
            } else {
                activeConnection.setRequestMethod(methodType.name());
            }
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

    private <T> T handleServerResponse(final String url, final Class<T> responseConversionClass, HttpMethodType methodType, RequestParam[] params) {
        String responseText = getResponseText(0, methodType, params);
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
                    String responseTextToShow = responseText.length() > 400
                            ? responseText.substring(0, 200) + "\n...\n" + responseText.substring(responseText.length() - 200)
                            : responseText;
                    log.error("Failed to parse response text for {} {}\n{}", activeConnection.getRequestMethod(), activeConnection.getURL(), responseTextToShow);
                    throw e;
                }
            }
        }
    }

    private void addCookiesHeader(String host) {
        String cookieHeaderValue = cookieFileStore.toCookieRequestText(host, useSessionCookies);
        if (StringUtils.isEmpty(cookieHeaderValue)) {
            return;
        }
        log.debug("Adding request header Cookie:{}", cookieHeaderValue);
        activeConnection.setRequestProperty("Cookie", cookieHeaderValue);
    }

    private String getResponseText(int retryCount, HttpMethodType methodType, RequestParam... params) {
        String responseText = "";
        try {
            responseText = parseResponseText(methodType);
            cookieFileStore.addCookiesFromResponse(activeConnection);
        } catch (SSLException e) {
            String urlText = activeConnection.getURL().toString();
            log.error("Ssl error for {} {}", activeConnection.getRequestMethod(), urlText);
            log.error("Error [{}]" ,e.getMessage());
            if (workflowCertificateManager.isLastServerTrusted()) {
                log.info("Last server certificate was trusted, try using --disable-hostname-verification to workaround errors due to subject alternative name");
                System.exit(1);
            }
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
            String requestMethod = activeConnection.getRequestMethod();
            String url = activeConnection.getURL().toString();
            throw new RuntimeIOException(ioe, "Failed on {} for {}", requestMethod, url);
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
            log.info("Url {} is already trusted, no need to save cert to local keystore", uri);
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
                + "\nAre you connected to the correct network?"
                + "\nFailed to access host " + activeConnection.getURL().getHost());
    }

    private String parseResponseText(HttpMethodType methodType) throws IOException {
        String currentUrl = activeConnection.getURL().toString();
        int responseCode = activeConnection.getResponseCode();
        log.debug("{}: {} Response code {}", methodType.name(), currentUrl, responseCode);
        String responseText;
        try {
            if (ExceptionChecker.isStatusValid(responseCode) || activeConnection.getErrorStream() == null) {
                responseText = IOUtils.read(activeConnection.getInputStream());
            } else {
                responseText = IOUtils.read(activeConnection.getErrorStream());
            }
        } catch (IOException ioe) {
            if (!ExceptionChecker.isStatusValid(responseCode)) {
                responseText = ioe.getMessage();
            } else {
                throw ioe;
            }
        }


        log.trace("Response\n{}", responseText);
        ExceptionChecker.throwExceptionIfStatusIsNotValid(currentUrl, responseCode, methodType.name(), responseText);
        return responseText;
    }
}
