package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class ApiTokenRequest {
    @SerializedName("api_format")
    public String apiFormat = "json";

    public Map<String, String> policy = new HashMap<>();

    public String note = "WorkflowTools";
}
