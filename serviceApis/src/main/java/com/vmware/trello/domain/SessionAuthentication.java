package com.vmware.trello.domain;

public class SessionAuthentication {
    public String authentication;
    public String dsc;

    public SessionAuthentication(String authentication, String dsc) {
        this.authentication = authentication;
        this.dsc = dsc;
    }
}
