package com.vmware.rest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.ExceptionChecker;
import com.vmware.rest.json.ConfiguredGsonBuilder;
import com.vmware.rest.request.RequestBodyFactory;
import com.vmware.rest.request.RequestBodyHandling;
import com.vmware.utils.IOUtils;
import com.vmware.utils.ThreadUtils;
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
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.vmware.rest.HttpMethodType.GET;
import static com.vmware.rest.HttpMethodType.POST;
import static com.vmware.rest.HttpMethodType.PUT;
import static com.vmware.rest.HttpMethodType.DELETE;

/**
 * Using Java's HttpURLConnection instead of Apache HttpClient to cut down on jar size
 */
public class RestConnection {
    private static Logger log = LoggerFactory.getLogger(RestConnection.class.getName());
    private static int CONNECTION_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(25, TimeUnit.SECONDS);

    private final CookieFile cookieFile;
    private Gson gson;
    private RequestBodyHandling requestBodyHandling;
    private String authorizationHeader = null;
    private String authQueryString;
    private HttpURLConnection activeConnection;
    private boolean useSessionCookies;

    public RestConnection(RequestBodyHandling requestBodyHandling) throws IOException {
        this.requestBodyHandling = requestBodyHandling;
        this.gson = new ConfiguredGsonBuilder().build();

        String homeFolder = System.getProperty("user.home");
        cookieFile = new CookieFile(homeFolder);
    }

    public void updateServerTimeZone(TimeZone serverTimezone, String serverDateFormat) {
        this.gson = new ConfiguredGsonBuilder(serverTimezone, serverDateFormat).build();
    }

    public void setupBasicAuthHeader(final UsernamePasswordCredentials credentials) {
        String basicCredentials = DatatypeConverter.printBase64Binary(credentials.toString().getBytes());
        authorizationHeader = "Basic " + basicCredentials;
    }

    public void setAuthQueryString(String authQueryString) {
        this.authQueryString = authQueryString;
    }

    public <T> T get(String url, Class<T> responseConversionClass, List<NameValuePair> params) throws IOException, URISyntaxException {
        return get(url, responseConversionClass, params.toArray(new NameValuePair[params.size()]));
    }

    public <T> T get(String url, Class<T> responseConversionClass, String acceptMediaType, List<NameValuePair> params) throws IOException, URISyntaxException {
        return get(url, responseConversionClass, acceptMediaType, params.toArray(new NameValuePair[params.size()]));
    }

    public <T> T get(String url, Class<T> responseConversionClass, NameValuePair... params)
            throws IOException, URISyntaxException {
        return get(url,responseConversionClass, "application/json", params);
    }

    public <T> T get(String url, Class<T> responseConversionClass, String acceptMediaType, NameValuePair... params)
            throws IOException, URISyntaxException {
        String fullUrl = UriUtils.buildUrl(url, params);
        setupConnection(fullUrl, GET, acceptMediaType);
        return handleServerResponse(responseConversionClass);
    }

    public <T> T put(String url, Object requestObject, Class<T> responseConversionClass)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, PUT);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass);
    }

    public <T> T post(String url, Class<T> responseConversionClass, Object requestObject)
            throws IllegalAccessException, IOException, URISyntaxException {
        return post(url, responseConversionClass, requestObject);
    }

    public <T> T post(String url, Class<T> responseConversionClass, Object requestObject, NameValuePair... requestHeaders)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, POST, "application/json", requestHeaders);
        RequestBodyFactory.setRequestDataForConnection(this, requestObject);
        return handleServerResponse(responseConversionClass);
    }

    public <T> T put(String url, Object requestObject)
            throws URISyntaxException, IOException, IllegalAccessException {
        return put(url, requestObject, null);
    }

    public <T> T post(String url, Object requestObject)
            throws IllegalAccessException, IOException, URISyntaxException {
        return post(url, null, requestObject);
    }

    public <T> T post(String url, Object requestObject, NameValuePair... headers)
            throws URISyntaxException, IOException, IllegalAccessException {
        return post(url, null, requestObject, headers);
    }

    public <T> T delete(String url)
            throws URISyntaxException, IOException, IllegalAccessException {
        setupConnection(url, DELETE);
        return handleServerResponse(null);
    }

    public void setRequestBodyHandling(final RequestBodyHandling requestBodyHandling) {
        this.requestBodyHandling = requestBodyHandling;
    }

    public boolean hasCookie(ApiAuthentication ApiAuthentication) {
        Cookie cookie = cookieFile.getCookieByName(ApiAuthentication.getCookieName());
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

    private void setupConnection(String url, HttpMethodType methodType) throws IOException, URISyntaxException {
        setupConnection(url, methodType, "application/json");
    }

    private void setupConnection(String url, HttpMethodType methodType, String acceptMediaType, NameValuePair... headers) throws IOException, URISyntaxException {
        if (authQueryString != null) {
            url += !url.contains("?") ? "?" : "&";
            url += authQueryString;
        }
        URI uri = new URI(url);
        activeConnection = (HttpURLConnection) uri.toURL().openConnection();
        activeConnection.setDoInput(true);
        activeConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        activeConnection.setReadTimeout(CONNECTION_TIMEOUT);
        activeConnection.setInstanceFollowRedirects(false);
        activeConnection.setRequestMethod(methodType.name());
        activeConnection.setRequestProperty("Accept", acceptMediaType);
        for (NameValuePair header : headers) {
            log.debug("Adding request header {}:{}", header.getName(), header.getValue());
            activeConnection.setRequestProperty(header.getName(), header.getValue());
        }
        addAuthorizationHeaderIfNotNull();
        addCookiesHeader(uri.getHost());
    }

    private <T> T handleServerResponse(final Class<T> responseConversionClass) throws IOException {
        String responseText = getResponseText(0);
        activeConnection.disconnect();
        if (responseText.isEmpty() || responseConversionClass == null) {
            return null;
        } else {
            try {
                return gson.fromJson(responseText, responseConversionClass);
            } catch (JsonSyntaxException e) {
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
        activeConnection.setRequestProperty("Cookie", cookieFile.toCookieRequestText(host, useSessionCookies));
    }

    private void addAuthorizationHeaderIfNotNull() {
        if (authorizationHeader != null) {
            activeConnection.setRequestProperty("Authorization", authorizationHeader);
        }
    }

    private String getResponseText(int retryCount) throws IOException {
        String responseText = "";
        int MAX_RETRIES = 3;
        try {
            responseText = parseResponseText();
            cookieFile.addCookiesFromResponse(activeConnection);
        } catch (SSLException e) {
            String urlText = activeConnection.getURL().toString();
            log.error("Ssl error for {} {}", activeConnection.getRequestMethod(), urlText);
            log.error("Error [{}]" ,e.getMessage());
            if (retryCount >= MAX_RETRIES) {
                exitDueToSslExceptions(urlText);
            }
            ThreadUtils.sleep(TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
            log.info("");
            log.info("Retrying request {} of {}", ++retryCount, MAX_RETRIES);
            responseText = getResponseText(retryCount);
        } catch (UnknownHostException e) {
            handleNetworkException(e);
        } catch (SocketException e) {
            handleNetworkException(e);
        }
        return responseText;
    }

    private void handleNetworkException(IOException e) {
        log.info("");
        log.error("Unknown host exception thrown: {}", e.getMessage());
        log.error("Are you connected to the corporate network?");
        log.error("Failed to access host {}", activeConnection.getURL().getHost());
        System.exit(1);
    }

    private void exitDueToSslExceptions(String urlText) {
        log.info("");
        log.info("Still getting ssl errors, can't proceed");
        if (urlText.contains("reviewboard")) {
            log.info("Sometimes there are one off ssl exceptions with the reviewboard api, try rerunning your workflow");
        }
        System.exit(1);
    }

    private String parseResponseText() throws IOException {
        String responseText;
        int responseCode = activeConnection.getResponseCode();
        if (ExceptionChecker.isStatusValid(responseCode)) {
            responseText = IOUtils.read(activeConnection.getInputStream());
        } else {
            responseText = IOUtils.read(activeConnection.getErrorStream());
        }
        ExceptionChecker.throwExceptionIfStatusIsNotValid(responseCode, responseText);
        return responseText;
    }

}
