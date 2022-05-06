package com.vmware.chrome.domain;

public class ChromeTab {
    private String id;
    private String parentId;
    private String description;
    private String title;
    private String type;
    private String url;
    private String devtoolsFrontendUrl;
    private String webSocketDebuggerUrl;
    private String faviconUrl;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getWebSocketDebuggerUrl() {
        return webSocketDebuggerUrl;
    }
}
