package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.ForbiddenException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
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

    final public String baseUrl;
    final protected String apiUrl;
    final protected ApiAuthentication credentialsType;
    final private String username;

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
        connectionIsAuthenticated = ThreadUtils.retryFunctionUntilSucceeds((retryCount) -> {
            if (isConnectionAuthenticated()) {
                return true;
            }
            connectionIsAuthenticated = null;
            if (retryCount == 0) {
                displayInputMessageForFirstLoginFailure();
            }
            try {
                loginManually();
                return true;
            } catch (NotAuthorizedException | ForbiddenException | NotFoundException e) {
                connectionIsAuthenticated = false;
                throw e;
            }
        },this.getClass().getSimpleName() + " login", MAX_LOGIN_RETRIES);
    }

    public String getBaseUrl() {
        return baseUrl;
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
        File apiTokenFile = determineApiTokenFile(credentialsType);
        if (!apiTokenFile.exists()) {
            return null;
        }
        log.debug("Reading {} api token from file {}", credentialsType.name(), apiTokenFile.getPath());
        return IOUtils.read(apiTokenFile);
    }

    protected void saveApiToken(String apiToken, ApiAuthentication credentialsType) {
        String existingToken = readExistingApiToken(credentialsType);
        if (StringUtils.isEmpty(apiToken) || apiToken.equals(existingToken)) {
            return;
        }
        File apiTokenFile = determineApiTokenFile(credentialsType);

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

    protected void displayInputMessageForFirstLoginFailure() {
        String filePath = determineApiTokenFile(credentialsType).getPath();
        if (credentialsType.getCookieName() != null) {
            log.info("Valid {} cookie ({}) not found in file {}", credentialsType.name(),
                    credentialsType.getCookieName(), filePath);
        } else {
            log.info("Valid {} token not found in file {}", credentialsType.name(), filePath);
        }
    }

    protected File determineApiTokenFile(ApiAuthentication apiAuthentication) {
        String homeFolder = System.getProperty("user.home");
        File apiTokenFile = new File(homeFolder + "/" + apiAuthentication.getFileName());
        if (!apiTokenFile.exists()) {
            log.debug("Api token file {} does not exist", apiTokenFile.getPath());
        }
        return apiTokenFile;
    }
}
