package com.vmware.chrome.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiRequest {
    private static AtomicInteger idCounter = new AtomicInteger(1);
    private int id;

    private String method;

    private Map<String, Object> params = new HashMap<>();

    public ApiRequest() {}

    public ApiRequest(String method) {
        this(method, Collections.emptyMap());
    }

    public ApiRequest(String method, Map<String, Object> params) {
        this.id = idCounter.getAndIncrement();
        this.method = method;
        this.params.putAll(params);
    }

    public int getId() {
        return id;
    }

    public static ApiRequest navigate(String url) {
        return new ApiRequest("Page.navigate", Collections.singletonMap("url", url));
    }

    public static ApiRequest evaluate(String expression) {
        return new ApiRequest("Runtime.evaluate", Collections.singletonMap("expression", expression));
    }

    public static ApiRequest elementById(String elementId) {
        return evaluate(String.format("document.getElementById('%s')", elementId));
    }

    public static ApiRequest sendInput(String text) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "char");
        params.put("text", text);
        return new ApiRequest("Input.dispatchKeyEvent", params);
    }
}
