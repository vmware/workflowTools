package com.vmware.github;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.github.domain.PullMergeRequest;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.Review;
import com.vmware.github.domain.User;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.UrlParam;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputUtils;

public class Github extends AbstractRestService {

    public Github(String baseUrl, String username) {
        super(baseUrl, "", ApiAuthentication.github, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
                        .namingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        String apiToken = readExistingApiToken(ApiAuthentication.github);
        if (StringUtils.isNotBlank(apiToken)) {
            connection.addStatefulParam(RequestHeader.aBearerAuthHeader(apiToken));
        }
    }

    public PullRequest createPullRequest(PullRequestForUpdate pullRequest) {
        PullRequest createdRequest = post(pullRequestsUrl(pullRequest.repoOwner, pullRequest.headRepo), PullRequest.class, pullRequest);
        createdRequest.repoOwner = pullRequest.repoOwner;
        return createdRequest;
    }

    public Optional<PullRequest> getPullRequestForSourceAndTargetBranch(String ownerName, String repoName, String sourceBranch, String targetBranch) {
        PullRequest[] pullRequests = get(pullRequestsUrl(ownerName, repoName), PullRequest[].class,
                new UrlParam("base", sourceBranch), new UrlParam("head", targetBranch));
        return pullRequests.length > 0 ? Optional.of(pullRequests[0]) : Optional.empty();
    }

    public List<Review> getApprovedReviewsForPullRequest(PullRequest pullRequest) {
        return Arrays.stream(get(pullRequestUrl(pullRequest) + "/reviews", Review[].class))
                .filter(Review::isApproved).collect(Collectors.toList());
    }

    public PullRequest getPullRequest(String ownerName, String repoName, int pullNumber) {
        PullRequest pullRequest = get(pullRequestUrl(ownerName, repoName, pullNumber), PullRequest.class);
        pullRequest.repoOwner = ownerName;
        return pullRequest;
    }

    public void mergePullRequest(PullRequest pullRequest) {
        PullMergeRequest pullMergeRequest = new PullMergeRequest();
        pullMergeRequest.mergeMethod = "merge";
        post(pullRequestUrl(pullRequest) + "/merge", PullMergeRequest.class, pullMergeRequest);
    }

    public PullRequest updatePullRequest(PullRequestForUpdate pullRequest) {
        PullRequest updatedRequest = put(pullRequestUrl(pullRequest.repoOwner, pullRequest.headRepo, pullRequest.number), PullRequest.class, pullRequest);
        updatedRequest.repoOwner = pullRequest.repoOwner;
        return updatedRequest;
    }

    public void addReviewersToPullRequest(PullRequest pullRequest, Set<User> users) {
        if (users.isEmpty()) {
            return;
        }
        post(pullRequestUrl(pullRequest) + "/requested_reviewers", String.class, pullRequest);
    }

    public void removeReviewersFromPullRequest(PullRequest pullRequest, Set<User> users) {
        if (users.isEmpty()) {
            return;
        }
        delete(pullRequestUrl(pullRequest) + "/requested_reviewers", pullRequest, Collections.emptyList());
    }

    public ReleaseAsset[] getReleaseAssets(String releasePath) {
        return get(UrlUtils.addRelativePaths(apiUrl, releasePath, "assets"), ReleaseAsset[].class);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        get(apiUrl + "/user", String.class);
    }

    @Override
    protected void loginManually() {
        connection.removeStatefulParam(RequestHeader.AUTHORIZATION);
        log.info("Github uses personal access tokens for third party API access.");
        log.info("On the UI, go to Settings -> Developer Setting and create a new personal access token");
        String privateToken = InputUtils.readValueUntilNotBlank("Enter Personal Access Token");
        saveApiToken(privateToken, ApiAuthentication.github);
        connection.addStatefulParam(RequestHeader.aBearerAuthHeader(privateToken));
    }

    private String pullRequestUrl(PullRequest pullRequest) {
        return pullRequestUrl(pullRequest.repoOwner, pullRequest.headRepo(), pullRequest.number);
    }

    private String pullRequestUrl(String ownerName, String repoName, long pullNumber) {
        return repoUrl(ownerName, repoName) + "/pulls" + "/" + pullNumber;
    }

    private String pullRequestsUrl(String ownerName, String repoName) {
        return repoUrl(ownerName, repoName) + "/pulls";
    }

    private String repoUrl(String ownerName, String repoName) {
        return UrlUtils.addRelativePaths(apiUrl, "repos", ownerName, repoName);
    }
}
