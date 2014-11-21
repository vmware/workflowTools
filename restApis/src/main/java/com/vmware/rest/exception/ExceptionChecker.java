/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest.exception;

import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class ExceptionChecker {

    public static void throwExceptionIfStatusIsNotValid(String currentUrl, final int statusCode, final String responseText) {
        if (isStatusValid(statusCode)) {
            return;
        }
        LoggerFactory.getLogger(ExceptionChecker.class).error("Encountered error with {}", currentUrl);
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
