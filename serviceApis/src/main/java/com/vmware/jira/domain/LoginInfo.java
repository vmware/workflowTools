package com.vmware.jira.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.util.exception.RuntimeIOException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class LoginInfo {

    @SerializedName("os_username")
    public String usernane;

    @SerializedName("os_password")
    public String password;

    @SerializedName("os_cookie")
    public String cookie;

    @SerializedName("os_destination")
    public String osDestination;

    public LoginInfo(UsernamePasswordCredentials credentials) {
        this.usernane = credentials.getUsername();
        try {
            this.password = URLEncoder.encode(credentials.getPassword(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeIOException(e);
        }
        this.cookie = "true";
        this.osDestination = "";
    }
}
