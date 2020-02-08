package com.vmware.reviewboard.domain;

public class Link {
    private String href;
    private String method;
    private String title;

    public Link(Link anotherLink) {
        this.href = anotherLink.getHref();
        this.method = anotherLink.getMethod();
        this.title = anotherLink.getTitle();
    }

    public Link(final String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public String getMethod() {
        return method;
    }

    public String getTitle() {
        return title;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addPathParam(String value) {
        href += value + "/";
    }
}
