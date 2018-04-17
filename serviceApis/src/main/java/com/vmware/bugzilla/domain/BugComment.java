package com.vmware.bugzilla.domain;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a comment on a bug.
 */
public class BugComment {

    @SerializedName("comment_id")
    private Integer commentId;

    @SerializedName("body")
    private String comment;

    @SerializedName("name")
    private String author;

    private Date time;

    public BugComment() {}

    public BugComment(String comment, String author) {
        this.comment = comment;
        this.author = author;
        this.time = new Date();
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public String getComment() {
        return comment;
    }

    public String getAuthor() {
        return author;
    }

    public Date getTime() {
        return time;
    }
}
