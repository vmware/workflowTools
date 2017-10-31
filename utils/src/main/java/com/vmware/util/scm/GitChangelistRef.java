package com.vmware.util.scm;

public class GitChangelistRef {

    private String commitRef;
    private String changelistId;

    public GitChangelistRef(String commitRef, String changelistId) {
        this.commitRef = commitRef;
        this.changelistId = changelistId;
    }

    public String getCommitRef() {
        return commitRef;
    }

    public String getChangelistId() {
        return changelistId;
    }
}
