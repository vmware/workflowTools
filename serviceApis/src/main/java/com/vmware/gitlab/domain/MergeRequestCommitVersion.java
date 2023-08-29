package com.vmware.gitlab.domain;

import java.util.Date;

public class MergeRequestCommitVersion {
    public String headCommitSha;
    public String baseCommitSha;
    public String startCommitSha;
    public Date createdAt;
    public int mergeRequestId;
}
