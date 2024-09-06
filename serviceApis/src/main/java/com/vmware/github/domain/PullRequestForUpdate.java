package com.vmware.github.domain;
import com.google.gson.annotations.Expose;

import java.util.Date;

public class PullRequestForUpdate {
    public long id;
    public long number;
    public String state;
    public String title;
    public String body;
    public User user;
    @Expose(serialize = false, deserialize = false)
    public String repoOwner;
    public String headRepo;
    public String head;
    public String base;
    public boolean draft;
    public Date createdAt;
    public Date mergedAt;
    public String htmlUrl;
}
