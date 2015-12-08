package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

public class ProductInfo {

    @SerializedName("is_release")
    public boolean isRelease;

    public String version;
}
