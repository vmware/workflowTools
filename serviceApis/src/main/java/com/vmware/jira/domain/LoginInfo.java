package com.vmware.jira.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.rest.credentials.UsernamePasswordCredentials;

public class LoginInfo {

    @SerializedName("os_username")
    public String usernane;

    @SerializedName("os_password")
    public String password;

    @SerializedName("os_cookie")
    public String cookie;

    public LoginInfo(UsernamePasswordCredentials credentials) {
        this.usernane = credentials.getUsername();
        this.password = credentials.getPassword();
        this.cookie = "true";
    }
}
