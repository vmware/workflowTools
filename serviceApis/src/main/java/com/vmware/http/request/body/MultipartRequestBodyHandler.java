package com.vmware.http.request.body;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.http.HttpConnection;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.xmlrpc.MapObjectConverter;

public class MultipartRequestBodyHandler {

    private static final String LINE_FEED = "\r\n";
    private static final String TWO_HYPHENS = "--";
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private MapObjectConverter converter = new MapObjectConverter();

    public void writeObjectAsMultipart(final HttpConnection connection, final Object requestObject) {
        // creates a unique boundary based on time stamp
        String boundary = "**********" + System.currentTimeMillis();
        connection.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = null;
        try {
            outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"),
                    true);
            Map<String, Object> valuesToWrite = converter.toMap(requestObject);
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
        }
    }

    /**
     * Adds a form field to a multipart request
     *
     * @param name  field name
     * @param value field value
     */
    private void addStringPart(PrintWriter writer, String boundary, String name, String value) throws UnsupportedEncodingException {
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
     */
    private void addFilePart(PrintWriter writer, String boundary, OutputStream outputStream, String fieldName, byte[] uploadFileData)
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
}
