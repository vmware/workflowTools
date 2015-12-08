package com.vmware;

import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.request.RequestParam;
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
public abstract class AbstractRestService extends AbstractService {

    protected RestConnection connection;

    protected AbstractRestService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        super(baseUrl, apiPath, credentialsType, username);
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

}
