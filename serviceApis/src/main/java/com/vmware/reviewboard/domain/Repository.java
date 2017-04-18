package com.vmware.reviewboard.domain;

public class Repository extends BaseEntity {

    public int id;

    public String name;

    public String path;

    public String tool;

    public RepoType getRepoType() {
        return tool != null ? RepoType.fromValue(tool.toLowerCase()) : null;
    }

}
