package com.vmware.trello.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.http.credentials.UsernamePasswordCredentials;

public class LoginInfo {
    @SerializedName("user")
    public String username;
    public String password;

    public LoginInfo(UsernamePasswordCredentials credentials) {
        this.username = credentials.getUsername();
        this.password = credentials.getPassword();
    }
}
