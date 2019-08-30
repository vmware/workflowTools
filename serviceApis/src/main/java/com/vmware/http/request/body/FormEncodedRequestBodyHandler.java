package com.vmware.http.request.body;

import com.vmware.http.HttpConnection;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.xmlrpc.MapObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class FormEncodedRequestBodyHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public void writeObjectAsFormEncoded(final HttpConnection connection, final Object requestObject) {
        Map<String, Object> valuesToWrite;
        try {
            if (requestObject instanceof Map) {
                valuesToWrite = (Map<String, Object>) requestObject;
            } else {
                valuesToWrite = new MapObjectConverter().toMap(requestObject);
            }
            writeValuesAsFormEncoded(connection, valuesToWrite);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public void writeObjectAsUrlEncodedJson(final HttpConnection connection, final Object requestObject) {
        String jsonText = connection.toJson(requestObject);
        log.trace("Request Json: {}", jsonText);
        Map<String, Object> values = new HashMap<>();
        values.put("json", jsonText);
        try {
            writeValuesAsFormEncoded(connection, values);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private void writeValuesAsFormEncoded(HttpConnection connection, Map<String, Object> valuesToWrite) throws IOException {
        if (!connection.containsRequestHeader("Content-Type")) {
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        }
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        String contentToWrite = "";
        for (String name : valuesToWrite.keySet()) {

            if (!contentToWrite.isEmpty()) {
                contentToWrite += "&";
            }
            String stringValue = String.valueOf(valuesToWrite.get(name));
            String encodedName = URLEncoder.encode(name, "UTF-8");
            contentToWrite += encodedName + "=" + stringValue;
        }
        log.trace("Form encoded request\n{}", contentToWrite);
        outputStream.writeBytes(contentToWrite);
        outputStream.flush();
        outputStream.close();
    }
}
