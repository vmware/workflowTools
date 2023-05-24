package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ApiTokenRequest {
    @SerializedName("api_format")
    public String apiFormat = "json";

    public Map<String, String> policy = new HashMap<>();

    public String note;

    public ApiTokenRequest() {
        this.note = "Created by Workflow Tools on " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
