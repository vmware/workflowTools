/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.http.request.body;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.http.HttpConnection;

public class RequestBodyFactory {
    private static final Logger log = LoggerFactory.getLogger(RequestBodyFactory.class);

    public static void setRequestDataForConnection(HttpConnection connection, final Object requestObject) {
        if (requestObject == null) {
            return;
        }
        connection.setDoOutput(true);
        switch (connection.getRequestBodyHandling()) {
            case AsStringJsonEntity:
                new JsonRequestBodyHandler().writeObjectAsJsonText(connection, requestObject);
                break;
            case AsUrlEncodedJsonEntity:
                new FormEncodedRequestBodyHandler().writeObjectAsUrlEncodedJson(connection, requestObject);
                break;
            case AsUrlEncodedFormEntity:
                if (requestObjectNeedsToUseMultipartEntity(requestObject)) {
                    new MultipartRequestBodyHandler().writeObjectAsMultipart(connection, requestObject);
                } else {
                    new FormEncodedRequestBodyHandler().writeObjectAsFormEncoded(connection, requestObject);
                }
                break;
            case AsMultiPartFormEntity:
                new MultipartRequestBodyHandler().writeObjectAsMultipart(connection, requestObject);
                break;
            default:
                throw new RuntimeException("No handling available for request body type "
                        + connection.getRequestBodyHandling());
        }
    }

    private static boolean requestObjectNeedsToUseMultipartEntity(Object requestObject) {
        for (Field field : requestObject.getClass().getDeclaredFields()) {
            if (field.getType() == byte[].class) {
                return true;
            }
        }
        return false;
    }

}
