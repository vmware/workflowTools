package com.vmware.reviewboard.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ReviewComment extends BaseEntity {
    public int id;

    @SerializedName("issue_opened")
    public boolean issueOpened;

    @SerializedName("public")
    public boolean isPublic;

    public String text;

    public Date timestamp;

    @SerializedName("first_line")
    public int firstLine;

    @SerializedName("num_lines")
    public int numLines;

    @SerializedName("issue_status")
    public DiffCommentStatus status;

    public String fileNameAndLineNumber() {
        String fileDiffTitle = getLink("filediff").getTitle();
        String className = fileDiffTitle.substring(0, fileDiffTitle.indexOf(" ("));
        return className + " : " + firstLine + " - " + numLines + " lines";
    }
}
