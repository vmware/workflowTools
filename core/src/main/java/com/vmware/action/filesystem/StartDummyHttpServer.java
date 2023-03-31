package com.vmware.action.filesystem;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.RuntimeIOException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@ActionDescription("Useful for testing load balancing, server just returns an error status")
public class StartDummyHttpServer extends BaseAction {

    public StartDummyHttpServer(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        try {
            log.info("Starting dummy http server on port {} returning status code {}", fileSystemConfig.httpServerPort, fileSystemConfig.httpServerStatusCode);
            HttpServer server = HttpServer.create(new InetSocketAddress(fileSystemConfig.httpServerPort), 0);
            server.createContext("/", new DefaultHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

    }

    private class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Sending " + fileSystemConfig.httpServerStatusCode + " for request " + t.getRequestURI();
            log.info("Response: {}", response);
            t.sendResponseHeaders(fileSystemConfig.httpServerStatusCode, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
