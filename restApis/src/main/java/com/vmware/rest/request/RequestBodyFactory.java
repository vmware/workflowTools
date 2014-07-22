/*
 * Project Horizon
 * (c) 2013 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.rest.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.rest.RestConnection;
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

    public static void setRequestDataForConnection(RestConnection connection, final Object requestObject)
            throws IllegalAccessException, IOException {
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

    private static void writeObjectAsMultipart(final RestConnection connection, final Object requestObject)
            throws IllegalAccessException, IOException {
        // creates a unique boundary based on time stamp
        String boundary = "===" + System.currentTimeMillis() + "===";
        connection.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = connection.getOutputStream();
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
        writer.append(LINE_FEED).flush();
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();
    }

    private static void writeObjectAsUrlEncodedJson(final RestConnection connection, final Object requestObject)
            throws IOException {
        String jsonText = connection.toJson(requestObject);
        log.trace("Request Json: {}", jsonText);
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("json", jsonText);
        writeValuesAsFormEncoded(connection, values);
    }

    private static void writeObjectAsJsonString(final RestConnection connection, final Object requestObject)
            throws IOException {
        String jsonText = connection.toJson(requestObject);
        log.trace("Request Json: {}", jsonText);
        connection.setRequestProperty("Content-Type", "application/json");
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
        writer.write(jsonText);
        writer.close();
    }

    private static void writeObjectAsFormEncoded(final RestConnection connection, final Object requestObject)
            throws IllegalAccessException, IOException {
        Map<String, Object> valuesToWrite = convertObjectToMap(requestObject);
        writeValuesAsFormEncoded(connection, valuesToWrite);
    }

    private static Map<String, Object> convertObjectToMap(Object requestObject) throws IllegalAccessException {
        Map<String, Object> valuesToWrite = new HashMap<String, Object>();
        for (Field field : requestObject.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
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

    private static void writeValuesAsFormEncoded(RestConnection connection, Map<String, Object> valuesToWrite) throws IOException {
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
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=UTF-8").append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(URLEncoder.encode(value, "UTF-8")).append(LINE_FEED);
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
        writer.append("--").append(boundary).append(LINE_FEED);
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
