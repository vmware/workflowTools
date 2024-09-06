package com.vmware;

import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.ApiException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.RequestParam;
import com.vmware.http.exception.ForbiddenException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.RuntimeIOException;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    protected void post(String url, Object requestBody, RequestParam... params) {
        post(url, null, requestBody, params);
    }

    /**
     * WIll try first to post the request.
     * If that fails, will then try to authenticate and re post the request.
     */
    protected <T> T post(String url, Class<T> responseConversionClass, Object requestBody, RequestParam... params) {
        return post(url, responseConversionClass, requestBody, Collections.emptyList(), params);
    }


    protected <T> T post(String url, Class<T> responseConversionClass, Object requestBody,
                         List<Class<? extends ApiException>> allowedExceptionTypes, RequestParam... params) {
        try {
            return connection.post(url, responseConversionClass, requestBody, params);
        } catch (NotAuthorizedException | ForbiddenException e) {
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
    protected <T> T get(String url, Class<T> responseConversionClass, RequestParam... params) {
        try {
            return connection.get(url, responseConversionClass, params);
        } catch (NotAuthorizedException | ForbiddenException e) {
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.get(url, responseConversionClass, params);
        } catch (NotFoundException | RuntimeIOException e) {
            ThreadUtils.sleep(3, TimeUnit.SECONDS);
            log.info("Retrying GET for url " + url);
            return connection.get(url, responseConversionClass, params);
        }
    }

    protected <T> T put(String url, Class<T> responseConversionClass, Object requestBody, RequestParam... params) {
        return put(url, responseConversionClass, requestBody, Collections.emptyList(), params);
    }

    protected <T> T put(String url, Class<T> responseConversionClass, Object requestBody, List<Class<? extends ApiException>> allowedExceptions,
                        RequestParam... params) {
        try {
            return connection.put(url, responseConversionClass, requestBody, params);
        } catch (NotAuthorizedException | ForbiddenException e) {
            if (allowedExceptions.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.put(url, responseConversionClass, requestBody, params);
        }
    }

    protected <T> T patch(String url, Class<T> responseConversionClass, Object requestBody, List<Class<? extends ApiException>> allowedExceptions,
                        RequestParam... params) {
        try {
            return connection.patch(url, responseConversionClass, requestBody, params);
        } catch (NotAuthorizedException | ForbiddenException e) {
            if (allowedExceptions.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.patch(url, responseConversionClass, requestBody, params);
        }
    }

    protected <T> T delete(String url, RequestParam... params) {
        return delete(url, null, Collections.emptyList(), params);
    }

    protected <T> T delete(String url, Object requestBody,
                           List<Class<? extends ApiException>> allowedExceptionTypes, RequestParam... params) {
        try {
            return connection.delete(url, requestBody, null, params);
        } catch (NotAuthorizedException | ForbiddenException e) {
            if (allowedExceptionTypes.contains(e.getClass())) {
                throw e;
            }
            connectionIsAuthenticated = false;
            setupAuthenticatedConnection();
            return connection.delete(url, params);
        }
    }
}
