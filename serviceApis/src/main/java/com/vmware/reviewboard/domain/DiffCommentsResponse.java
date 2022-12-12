package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

public class DiffCommentsResponse extends BaseResponseEntity {

    @SerializedName("diff_comments")
    public ReviewComment[] diffComments;
}
