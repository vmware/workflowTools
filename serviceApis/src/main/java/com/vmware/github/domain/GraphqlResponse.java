package com.vmware.github.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GraphqlResponse {
    public enum PullRequestReviewDecision {
        APPROVED, CHANGES_REQUEST, REVIEW_REQUIRED
    }
    public ResponseData data;

    public static class ResponseData {
        public Repository repository;
        public PullRequestNode pullRequest;
        public Search search;
    }

    public static class Search {
        @SerializedName("userCount")
        public int userCount;
        public UserNode[] edges;

        public List<User> usersForCompany(String companyName) {
            return Arrays.stream(edges).map(edge -> edge.node).filter(user -> userBelongsToCompany(user, companyName)).collect(Collectors.toList());
        }

        private boolean userBelongsToCompany(User user, String companyName) {
            if (StringUtils.isEmpty(companyName)) {
                return true;
            } else {
                return user.organization != null && companyName.equalsIgnoreCase(user.organization.login);
            }
        }
    }

    public static class Repository {
        @SerializedName("pullRequest")
        public PullRequestNode pullRequest;
    }

    public static class PullRequestNode {
        @SerializedName("reviewDecision")
        public PullRequestReviewDecision reviewDecision;
        @SerializedName("reviewThreads")
        public ReviewThreadNodes reviewThreads;
        @SerializedName("reviews")
        public ReviewNodes approvedReviews;

        public double number;
        @SerializedName("isDraft")
        public boolean isDraft;
        public boolean closed;

        public List<String> approvers() {
            return Arrays.stream(approvedReviews.nodes).map(node -> node.author.login).collect(Collectors.toList());
        }
    }

    public static class UserNode {
        public User node;
    }
}
