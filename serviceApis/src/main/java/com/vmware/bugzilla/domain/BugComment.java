package com.vmware.bugzilla.domain;

import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Represents a comment on a bug.
 */
public class BugComment {

    private Integer commentId;

    private String comment;

    private String author;

    private Date time;

    public BugComment(String comment, String author) {
        this.comment = comment;
        this.author = author;
        this.time = new Date();
    }

    public BugComment(Map values) throws IOException {
        this.commentId = (Integer) values.get("comment_id");
        this.comment = StringUtils.convertObjectToString(values.get("body"));
        this.author = (String) values.get("name");
        this.time = (Date) values.get("time");
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
