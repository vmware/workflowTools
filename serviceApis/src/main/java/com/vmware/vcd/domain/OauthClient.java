package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OauthClient {
    @SerializedName("client_id")
    public String clientId;
    @SerializedName("client_name")
    public String clientName;
    @SerializedName("grant_types")
    public List<String> grantTypes;

    public String jwtGrantRequest() {
        return grantTypes.stream().filter(grantType -> grantType.contains("jwt")).findFirst()
                .orElseThrow(() -> new RuntimeException("No jet grant type found in " + grantTypes));
    }
}
