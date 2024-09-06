package com.vmware.github.domain;
import com.google.gson.annotations.Expose;

public class PullRequestForUpdate {
    @Expose(serialize = false, deserialize = false)
    public long number;
    public String state;
    public String title;
    public String body;
    @Expose(serialize = false, deserialize = false)
    public String repoOwner;
    @Expose(serialize = false, deserialize = false)
    public String repoName;
    public String head;
    public String base;
    public boolean draft;
}
