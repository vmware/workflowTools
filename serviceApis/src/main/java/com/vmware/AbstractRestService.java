package com.vmware;

import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.ApiException;
import com.vmware.http.request.RequestParam;
import com.vmware.http.exception.ForbiddenException;
import com.vmware.http.exception.NotAuthorizedException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all rest services.
 * Subclasses need to implement the authentication methods.
 */
public abstract class AbstractRestService extends AbstractService {

    protected HttpConnection connection;

    protected AbstractRestService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        super(baseUrl, apiPath, credentialsType, username);
    }

    @Override
    public boolean isBaseUriTrusted() {
        return connection.isUriTrusted(URI.create(baseUrl));
    }

    protected void optimisticPost(String url, Object requestBody, RequestParam... params) {
        optimisticPost(url, null, requestBody, params);
    }

    /**
     * WIll try first to post the request.
     * If that fails, will then try to authenticate and re post the request.
     */
    protected <T> T optimisticPost(String url, Class<T> responseConversionClass, Object requestBody, RequestParam... params) {
        return optimisticPost(url, responseConversionClass, requestBody, Collections.emptyList(), params);
    }


    protected <T> T optimisticPost(String url, Class<T> responseConversionClass, Object requestBody,
                                   List<Class<? extends ApiException>> allowedExceptionTypes, RequestParam... params) {
        try {
            return connection.post(url, responseConversionClass, requestBody, params);
        } catch (NotAuthorizedException e) {
            if (allowedExceptionTypes.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.post(url, requestBody, params);
        } catch (ForbiddenException e) {
            if (allowedExceptionTypes.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.post(url, requestBody, params);
        }
    }

    /**
     * WIll try first to get the request.
     * If that fails, will then try to authenticate and re get the request.
     */
    protected <T> T optimisticGet(String url, Class<T> responseConversionClass, RequestParam... params) {
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

    protected <T> T optimisticPut(String url, Class<T> responseConversionClass, Object requestBody, RequestParam... params) {
        return optimisticPut(url, responseConversionClass, requestBody, Collections.emptyList(), params);
    }

    protected <T> T optimisticPut(String url, Class<T> responseConversionClass, Object requestBody, List<Class<? extends ApiException>> allowedExceptions,
                                  RequestParam... params) {
        try {
            return connection.put(url, responseConversionClass, requestBody, params);
        } catch (NotAuthorizedException e) {
            if (allowedExceptions.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.put(url, requestBody, params);
        } catch (ForbiddenException e) {
            if (allowedExceptions.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.put(url, requestBody, params);
        }
    }

}
