package com.vmware.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the response body and headers
 */
public class HttpResponse {

    public HttpResponse(String content, Map<String, List<String>> headers) {
        this.content = content;
        this.headers = headers;
    }

    private String content;

    private Map<String, List<String>> headers;

    public String getContent() {
        return content;
    }

    public Map<String, List<String>> getHeaders() {
        return new HashMap<>(headers);
    }

    public boolean containsLink(String rel, String model) {
        List<String> links = headers.get("Link");
        return links.stream().anyMatch(link -> link.contains("rel=\"" + rel + "\"") && link.contains("model=\"" + model + "\""));
    }
}
