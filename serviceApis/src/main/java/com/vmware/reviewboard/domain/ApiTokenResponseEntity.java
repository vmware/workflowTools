package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

public class ApiTokenResponseEntity extends BaseResponseEntity {

    @SerializedName("api_token")
    public ApiToken apiToken;
}
