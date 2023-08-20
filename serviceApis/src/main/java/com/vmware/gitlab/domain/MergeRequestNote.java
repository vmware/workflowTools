package com.vmware.gitlab.domain;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class MergeRequestNote {
    public long id;
    public String body;
    public User author;
    public Type type;
    public boolean resolvable;
    public boolean resolved;
    @SerializedName("created_at")
    public Date createdAt;

    @SerializedName("noteable_type")
    public String noteableType;

    public enum Type {
        DiffNote;
    }
}
