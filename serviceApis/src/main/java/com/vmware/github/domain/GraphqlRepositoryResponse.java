package com.vmware.github.domain;

import com.google.gson.annotations.SerializedName;

public class GraphqlRepositoryResponse {
    public RepositoryData data;

    public class RepositoryData {
        public Repository repository;
    }

    public class Repository {
        @SerializedName("pullRequest")
        public PullRequestNode pullRequest;
    }

    public class PullRequestNode {
        @SerializedName("reviewThreads")
        public ReviewThreadNodes reviewThreads;
    }
}
