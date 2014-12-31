package com.vmware;

import com.vmware.rest.ApiAuthentication;
import com.vmware.rest.RequestParam;
import com.vmware.rest.RestConnection;
import com.vmware.rest.UrlUtils;
import com.vmware.rest.exception.ForbiddenException;
import com.vmware.rest.exception.NotAuthorizedException;
import com.vmware.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Base class for all rest services.
 * Subclasses need to implement the authentication methods.
 */
public abstract class AbstractRestService {

    private final static int MAX_LOGIN_RETRIES = 5;

    protected Boolean connectionIsAuthenticated = null;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected RestConnection connection;
    protected String baseUrl;

    public String apiUrl;
    protected ApiAuthentication credentialsType;
    protected String username;

    protected AbstractRestService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        this.baseUrl = UrlUtils.addTrailingSlash(baseUrl);
        this.apiUrl = this.baseUrl + apiPath;
        this.credentialsType = credentialsType;
        this.username = username;
    }

    /**
     * Subclasses should implement this method as an authentication check.
     * An UnauthorizedException should be thrown if authentication fails
     */
    protected abstract void checkAuthenticationAgainstServer() throws IOException, URISyntaxException;

    /**
     * Ask the user for credentials and retrieve a token / cookie for future authentication. This should be persisted.
     */
    protected abstract void loginManually() throws IllegalAccessException, IOException, URISyntaxException;

    public boolean isConnectionAuthenticated() throws IOException, URISyntaxException {
        if (connectionIsAuthenticated != null) {
            return connectionIsAuthenticated;
        }
        try {
            checkAuthenticationAgainstServer();
            connectionIsAuthenticated = true;
        } catch (NotAuthorizedException e) {
            connectionIsAuthenticated = false;
        } catch (ForbiddenException e) {
            connectionIsAuthenticated = false;
        }
        return connectionIsAuthenticated;
    }

    public void setupAuthenticatedConnection() throws IOException, URISyntaxException, IllegalAccessException {
        int retryCount = 0;
        while (!isConnectionAuthenticated()) {
            if (retryCount > MAX_LOGIN_RETRIES) {
                System.exit(1);
            }
            connectionIsAuthenticated = null;
            displayInputMessage(retryCount);
            try {
                loginManually();
            } catch (NotAuthorizedException e) {
                connectionIsAuthenticated = false;
            }
            retryCount++;
        }
    }

    private void displayInputMessage(int retryCount) {
        if (retryCount == 0) {
            String homeFolder = System.getProperty("user.home");
            String filePath = homeFolder + "/" + credentialsType.getFileName();
            if (credentialsType.getCookieName() != null) {
                log.info("Valid {} cookie ({}) not found in file {}", credentialsType.name(),credentialsType.getCookieName(), filePath);
            } else {
                log.info("Valid {} token not found in file {}", credentialsType.name(), filePath);
            }
        } else  {
            log.info("");
            log.warn("Login failure");
            log.info("Retrying login, attempt {} of {}", retryCount, MAX_LOGIN_RETRIES);
        }
    }

    /**
     * WIll try first to post the request.
     * If that fails, will then try to authenticate and re post the request.
     */
    protected void optimisticPost(String url, Object requestBody, RequestParam... params) throws IllegalAccessException, IOException, URISyntaxException {
        try {
            connection.post(url, requestBody, params);
        } catch (NotAuthorizedException e) {
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            connection.post(url, requestBody, params);
        } catch (ForbiddenException e) {
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            connection.post(url, requestBody, params);
        }
    }

    /**
     * WIll try first to get the request.
     * If that fails, will then try to authenticate and re get the request.
     */
    protected <T> T optimisticGet(String url, Class<T> responseConversionClass, RequestParam... params) throws IOException, URISyntaxException, IllegalAccessException {
        try {
            return connection.get(url, responseConversionClass, params);
        } catch (NotAuthorizedException e) {
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.get(url, responseConversionClass, params);
        } catch (ForbiddenException e) {
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.get(url, responseConversionClass, params);
        }
    }

    /**
     * Api tokens are stored in the user's home directly.
     * @see ApiAuthentication for file system locations
     */
    protected String readExistingApiToken() throws IOException {
        String homeFolder = System.getProperty("user.home");
        File apiTokenFile = new File(homeFolder + "/" + credentialsType.getFileName());
        if (!apiTokenFile.exists()) {
            return null;
        }
        log.debug("Reading {} api token from file {}", credentialsType.name(), apiTokenFile.getPath());
        return IOUtils.read(new FileInputStream(apiTokenFile));
    }

    protected void saveApiToken(String apiToken) throws IOException {
        String existingToken = readExistingApiToken();
        if (apiToken == null || apiToken.equals(existingToken)) {
            return;
        }
        String homeFolder = System.getProperty("user.home");
        File apiTokenFile = new File(homeFolder + "/" + credentialsType.getFileName());

        log.info("Saving {} api token to {}", credentialsType.name(), apiTokenFile.getPath());
        IOUtils.write(apiTokenFile, apiToken);
    }

}
