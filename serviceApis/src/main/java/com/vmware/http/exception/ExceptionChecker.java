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

        String fullResponseText = currentUrl + "\n" + responseText;

        switch (statusCode) {
            case DoesNotExistException.STATUS_CODE:
                throw new DoesNotExistException(fullResponseText);
            case PermissionDeniedException.STATUS_CODE:
                throw new PermissionDeniedException(fullResponseText);
            case NotLoggedInException.STATUS_CODE:
                throw new NotLoggedInException(fullResponseText);
            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw new BadRequestException(fullResponseText);
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new NotAuthorizedException(fullResponseText);
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw new ForbiddenException(fullResponseText);
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new NotFoundException(fullResponseText);
            case HttpURLConnection.HTTP_BAD_METHOD:
                throw new MethodNotAllowedException(fullResponseText);
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw new InternalServerException(fullResponseText);
            default:
                throw new UnexpectedStatusException(statusCode, fullResponseText);
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
