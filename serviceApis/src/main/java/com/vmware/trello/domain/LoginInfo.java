package com.vmware.trello.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.http.credentials.UsernamePasswordCredentials;

public class LoginInfo {
    public String method = "password";

    @SerializedName("factors[user]")
    public String username;
    @SerializedName("factors[password]")
    public String password;

    public LoginInfo(UsernamePasswordCredentials credentials) {
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
    }
}
