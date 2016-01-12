/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.http.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.http.HttpConnection;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class RequestBodyFactory {
    private static final Logger log = LoggerFactory.getLogger(RequestBodyFactory.class);
    private static final String LINE_FEED = "\r\n";
    private static final String TWO_HYPHENS = "--";

    public static void setRequestDataForConnection(HttpConnection connection, final Object requestObject) {
        connection.setDoOutput(true);
        switch (connection.getRequestBodyHandling()) {
            case AsStringJsonEntity:
                writeObjectAsJsonString(connection, requestObject);
                break;
            case AsUrlEncodedJsonEntity:
                writeObjectAsUrlEncodedJson(connection, requestObject);
                break;
            case AsUrlEncodedFormEntity:
                if (requestObjectNeedsToUseMultipartEntity(requestObject)) {
                    writeObjectAsMultipart(connection, requestObject);
                } else {
                    writeObjectAsFormEncoded(connection, requestObject);
                }
                break;
            case AsMultiPartFormEntity:
                writeObjectAsMultipart(connection, requestObject);
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

    private static void writeObjectAsMultipart(final HttpConnection connection, final Object requestObject) {
        // creates a unique boundary based on time stamp
        String boundary = "**********" + System.currentTimeMillis();
        connection.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"),
                    true);
            Map<String, Object> valuesToWrite = convertObjectToMap(requestObject);
            for (String fieldName : valuesToWrite.keySet()) {
                Object value = valuesToWrite.get(fieldName);
                if (value instanceof byte[]) {
                    byte[] binaryData = (byte[]) value;
                    addFilePart(writer, boundary, outputStream, fieldName, binaryData);
                } else {
                    addStringPart(writer, boundary, fieldName, String.valueOf(value));
                }
            }
            writer.append(LINE_FEED);
            log.trace(TWO_HYPHENS + boundary + TWO_HYPHENS);
            writer.append(TWO_HYPHENS).append(boundary).append(TWO_HYPHENS).append(LINE_FEED).flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }

    }

    private static void writeObjectAsUrlEncodedJson(final HttpConnection connection, final Object requestObject) {
        String jsonText = connection.toJson(requestObject);
        log.trace("Request Json: {}", jsonText);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("json", jsonText);
        try {
            writeValuesAsFormEncoded(connection, values);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static void writeObjectAsJsonString(final HttpConnection connection, final Object requestObject) {
        String jsonText = connection.toJson(requestObject);
        log.trace("Request Json: {}", jsonText);
        connection.setRequestProperty("Content-Type", "application/json");
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(jsonText);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static void writeObjectAsFormEncoded(final HttpConnection connection, final Object requestObject) {
        Map<String, Object> valuesToWrite = null;
        try {
            valuesToWrite = convertObjectToMap(requestObject);
            writeValuesAsFormEncoded(connection, valuesToWrite);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    private static Map<String, Object> convertObjectToMap(Object requestObject) throws IllegalAccessException {
        Map<String, Object> valuesToWrite = new HashMap<String, Object>();
        for (Field field : requestObject.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (Modifier.isPrivate(field.getModifiers())) {
                continue;
            }
            Expose expose = field.getAnnotation(Expose.class);
            if (expose != null && !expose.serialize()) {
                continue;
            }
            Object value = field.get(requestObject);
            if (value == null) {
                continue;
            }
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            String nameToUse = serializedName != null ? serializedName.value() : field.getName();
            Object valueToUse = determineCorrectValue(value);
            if (valueToUse != null) {
                valuesToWrite.put(nameToUse, valueToUse);
            }
        }
        return valuesToWrite;
    }

    private static void writeValuesAsFormEncoded(HttpConnection connection, Map<String, Object> valuesToWrite) throws IOException {
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        String contentToWrite = "";
        for (String name : valuesToWrite.keySet()) {

            if (!contentToWrite.isEmpty()) {
                contentToWrite += "&";
            }
            String stringValue = String.valueOf(valuesToWrite.get(name));
            String encodedValue = URLEncoder.encode(stringValue,  "UTF-8");
            contentToWrite += name + "=" + encodedValue;
        }
        log.trace("Form encoded request\n{}", contentToWrite);
        outputStream.writeBytes(contentToWrite);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Adds a form field to a multipart request
     *
     * @param name  field name
     * @param value field value
     */
    public static void addStringPart(PrintWriter writer, String boundary, String name, String value) throws UnsupportedEncodingException {
        StringBuilder contentToAdd = new StringBuilder();

        contentToAdd.append(TWO_HYPHENS).append(boundary).append(LINE_FEED);
        contentToAdd.append("Content-Disposition: form-data; name=\"").append(name).append("\"")
                .append(LINE_FEED);
        contentToAdd.append("Content-Type: text/plain; charset=UTF-8").append(
                LINE_FEED);
        contentToAdd.append(LINE_FEED);
        contentToAdd.append(value).append(LINE_FEED);
        String content = contentToAdd.toString();
        writer.append(content);
        log.trace(content);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFileData data to be uploaded
     * @throws IOException
     */
    public static void addFilePart(PrintWriter writer, String boundary, OutputStream outputStream, String fieldName, byte[] uploadFileData)
            throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(uploadFileData);
        writer.append(TWO_HYPHENS).append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(fieldName)
                .append("\"; filename=\"").append(fieldName).append("\"")
                .append(LINE_FEED);
        writer.append("Content-Type: application/octet-stream")
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

    private static Object determineCorrectValue(Object objectToCheck)
            throws IllegalAccessException {
        if (objectToCheck instanceof byte[]) {
            return objectToCheck;
        } else if (objectToCheck instanceof Boolean) {
            Boolean bool = (Boolean) objectToCheck;
            return bool ? "1" : "0";
        }
        return objectToCheck.toString();
    }

}
