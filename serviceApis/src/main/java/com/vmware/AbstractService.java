package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.ForbiddenException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Base Class for Rest and Non Rest services.
 */
public abstract class AbstractService {

    private final static int MAX_LOGIN_RETRIES = 3;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public String baseUrl;
    protected String apiUrl;
    protected ApiAuthentication credentialsType;
    private String username;

    protected Boolean connectionIsAuthenticated = null;

    protected AbstractService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        if (StringUtils.isEmpty(baseUrl)) {
            throw new FatalException("No url set for service {}", this.getClass().getSimpleName());
        }
        this.baseUrl = UrlUtils.addTrailingSlash(baseUrl);
        this.apiUrl = this.baseUrl + apiPath;
        this.credentialsType = credentialsType;
        this.username = username;
    }

    public boolean isConnectionAuthenticated() {
        if (connectionIsAuthenticated != null) {
            return connectionIsAuthenticated;
        }
        try {
            checkAuthenticationAgainstServer();
            connectionIsAuthenticated = true;
        } catch (NotAuthorizedException | ForbiddenException e) {
            connectionIsAuthenticated = false;
        }
        return connectionIsAuthenticated;
    }

    public void setupAuthenticatedConnection() {
        int retryCount = 0;
        while (!isConnectionAuthenticated()) {
            connectionIsAuthenticated = null;
            displayInputMessage(retryCount);
            try {
                loginManually();
            } catch (NotAuthorizedException | ForbiddenException | NotFoundException e) {
                if (retryCount >= MAX_LOGIN_RETRIES) {
                    throw e;
                }
                connectionIsAuthenticated = false;
            }
            retryCount++;
        }
    }

    public abstract boolean isBaseUriTrusted();

    public String getUsername() {
        if (StringUtils.isEmpty(username)) {
            throw new RuntimeException("Username is empty, please set workflow config value username or git config --global user.email [your email address]");
        }
        return username;
    }

    /**
     * Api tokens are stored in the user's home directly.
     * @see ApiAuthentication for file system locations
     */
    protected String readExistingApiToken(ApiAuthentication credentialsType) {
        File apiTokenFile = determineApiTokenFile();
        if (!apiTokenFile.exists()) {
            return null;
        }
        log.debug("Reading {} api token from file {}", credentialsType.name(), apiTokenFile.getPath());
        return IOUtils.read(apiTokenFile);
    }

    protected void saveApiToken(String apiToken, ApiAuthentication credentialsType) {
        String existingToken = readExistingApiToken(credentialsType);
        if (apiToken == null || apiToken.equals(existingToken)) {
            return;
        }
        File apiTokenFile = determineApiTokenFile();

        log.info("Saving {} api token to {}", credentialsType.name(), apiTokenFile.getPath());
        IOUtils.write(apiTokenFile, apiToken);
    }

    /**
     * Subclasses should implement this method as an authentication check.
     * An UnauthorizedException should be thrown if authentication fails
     */
    protected abstract void checkAuthenticationAgainstServer();

    /**
     * Ask the user for credentials and retrieve a token / cookie for future authentication. This should be persisted.
     */
    protected abstract void loginManually();

    private void displayInputMessage(int retryCount) {
        if (retryCount == 0) {
            String filePath = determineApiTokenFile().getPath();
            if (credentialsType.getCookieName() != null) {
                log.info("Valid {} cookie ({}) not found in file {}", credentialsType.name(),
                        credentialsType.getCookieName(), filePath);
            } else {
                log.info("Valid {} token not found in file {}", credentialsType.name(), filePath);
            }
        } else  {
            log.info("");
            log.warn("Login failure");
            log.info("Retrying login, attempt {} of {}", retryCount, MAX_LOGIN_RETRIES);
        }
    }

    protected File determineApiTokenFile() {
        String homeFolder = System.getProperty("user.home");
        return new File(homeFolder + "/" + credentialsType.getFileName());
    }
}
