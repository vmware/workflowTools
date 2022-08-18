package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;

@VcdMediaType("application/json")
public class OauthTokenRequest {
    @SerializedName("grant_type")
    public String grantType;

    @SerializedName("client_id")
    public String clientId;

    @SerializedName("refresh_token")
    public String refreshToken;

    public String assertion;

    public OauthTokenRequest() {
    }

    public OauthTokenRequest(String grantType, String clientId, String assertion) {
        this.grantType = grantType;
        this.clientId = clientId;
        this.assertion = assertion;
    }

    public OauthTokenRequest(String grantType, String refreshToken) {
        this.grantType = grantType;
        this.refreshToken = refreshToken;
    }
}
