package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;

@VcdMediaType("application/json")
public class OauthToken {
    @SerializedName("access_token")
    public String accessToken;
    @SerializedName("refresh_token")
    public String refreshToken;
}
