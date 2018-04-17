package com.vmware.http.request.body;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.http.HttpConnection;
import com.vmware.util.exception.RuntimeIOException;

public class JsonRequestBodyHandler {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public void writeObjectAsJsonText(final HttpConnection connection, final Object requestObject) {
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
}
