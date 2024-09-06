package com.vmware.github.domain;

public class CommitRef {
    public String label;
    public String ref;
    public String sha;
    public User user;

    public String repoName() {
        return label.split(":")[0];
    }
}
