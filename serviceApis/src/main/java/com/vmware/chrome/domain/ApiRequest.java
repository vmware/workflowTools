package com.vmware.chrome.domain;

import com.google.gson.annotations.Expose;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiRequest {
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private int id;

    private String method;

    private Map<String, Object> params = new HashMap<>();

    @Expose(serialize = false, deserialize = false)
    private String source;

    public ApiRequest() {}

    public ApiRequest(String method) {
        this(method, Collections.emptyMap(), null);

    }

    public ApiRequest(String method, Map<String, Object> params) {
        this(method, params, null);
    }

    public ApiRequest(String method, Map<String, Object> params, String source) {
        this.id = idCounter.getAndIncrement();
        this.method = method;
        this.params.putAll(params);
        this.source = source;
    }

    public int getId() {
        return id;
    }

    public static ApiRequest navigate(String url) {
        return new ApiRequest("Page.navigate", Collections.singletonMap("url", url));
    }

    public static ApiRequest evaluate(String expression) {
        return evaluate(expression, null);
    }

    public static ApiRequest evaluate(String expression, String source) {
        return new ApiRequest("Runtime.evaluate", Collections.singletonMap("expression", expression), source);
    }

    public static ApiRequest elementById(String elementId) {
        return evaluate(String.format("document.getElementById('%s')", elementId), elementId);
    }

    public static ApiRequest elementByXpath(String xpath) {
        return evaluate("document.evaluate(\"" + xpath + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE).singleNodeValue", xpath);
    }

    public String getExpression() {
        return (String) params.get("expression");
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static ApiRequest sendInput(String text) {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "char");
        params.put("text", text);
        return new ApiRequest("Input.dispatchKeyEvent", params);
    }
}
