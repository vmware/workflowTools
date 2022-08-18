package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;

public class OauthClientRegistration {

    @SerializedName("client_name")
    public String clientName;

    public OauthClientRegistration() {
    }

    public OauthClientRegistration(String clientName) {
        this.clientName = clientName;
    }
}
