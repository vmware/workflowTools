/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.http.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

import com.vmware.http.HttpMethodType;

public class ExceptionChecker {

    private static Logger log = LoggerFactory.getLogger(ExceptionChecker.class);

    public static void throwExceptionIfStatusIsNotValid(String currentUrl, final int statusCode, HttpMethodType methodType, final String responseText) {
        if (isStatusValid(statusCode)) {
            return;
        }
        log.debug("Response {} for {} url {}: {}", statusCode, methodType, currentUrl, responseText);

        switch (statusCode) {
            case DoesNotExistException.STATUS_CODE:
                throw new DoesNotExistException(responseText);
            case PermissionDeniedException.STATUS_CODE:
                throw new PermissionDeniedException(responseText);
            case NotLoggedInException.STATUS_CODE:
                throw new NotLoggedInException(responseText);
            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw new BadRequestException(responseText);
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new NotAuthorizedException(responseText);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new ForbiddenException(responseText);
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new NotFoundException(responseText);
            case HttpURLConnection.HTTP_BAD_METHOD:
                throw new MethodNotAllowedException(responseText);
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw new InternalServerException(responseText);
            default:
                throw new UnexpectedStatusException(statusCode, responseText);
        }
    }

    public static boolean isStatusValid(int statusCode) {
        if ((statusCode >= 200 && statusCode <= 206)
                || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            return true;
        }
        return false;
    }
}
