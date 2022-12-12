package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

public class GeneralCommentsResponse extends BaseResponseEntity {
    @SerializedName("general_comments")
    public ReviewComment[] generalComments;
}
