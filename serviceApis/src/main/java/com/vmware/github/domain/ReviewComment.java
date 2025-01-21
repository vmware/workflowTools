package com.vmware.github.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ReviewComment {
    public String id;
    @SerializedName("createdAt")
    public Date createdAt;
    public String body;
    @SerializedName("diffHunk")
    public String diffHunk;
    public Actor author;

    public Date getCreatedAt() {
        return createdAt;
    }
}
