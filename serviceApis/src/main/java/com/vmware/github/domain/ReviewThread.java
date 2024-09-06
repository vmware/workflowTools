package com.vmware.github.domain;

import com.google.gson.annotations.SerializedName;

public class ReviewThread {
    public String id;
    public String path;
    @SerializedName("isResolved")
    public boolean isResolved;
    public ReviewCommentNodes comments;

    public class ReviewCommentNodes {
        public ReviewComment[] nodes;
    }

}
