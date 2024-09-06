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
import com.vmware.github.domain.GraphqlRequest;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.github.domain.PullMergeRequest;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.GraphqlRepositoryResponse;
import com.vmware.github.domain.Review;
import com.vmware.github.domain.ReviewThread;
import com.vmware.github.domain.User;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.UrlParam;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.ClasspathResource;
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
        setupAuthenticatedConnection();
        return post(pullRequestsUrl(pullRequest.repoOwner, pullRequest.repoName), PullRequest.class, pullRequest);
    }

    public Optional<PullRequest> getPullRequestForSourceAndTargetBranch(String ownerName, String repoName, String sourceBranch, String targetBranch) {
        PullRequest[] pullRequests = get(pullRequestsUrl(ownerName, repoName), PullRequest[].class, new UrlParam("base", targetBranch));
        return Arrays.stream(pullRequests).filter(pullRequest -> pullRequest.head.ref.equals(sourceBranch)).findFirst();
    }

    public List<Review> getApprovedReviewsForPullRequest(PullRequest pullRequest) {
        return Arrays.stream(getReviewsForPullRequest(pullRequest)).filter(Review::isApproved).collect(Collectors.toList());
    }

    public Review[] getReviewsForPullRequest(PullRequest pullRequest) {
        return get(pullRequestUrl(pullRequest) + "/reviews", Review[].class);
    }

    public ReviewThread[] getReviewThreadsForPullRequest(PullRequest pullRequest) {
        String reviewThreadsQuery = new ClasspathResource("/githubReviewThreadsGraphql.txt", this.getClass()).getText();
        GraphqlRequest request = new GraphqlRequest();

        request.query = reviewThreadsQuery.replace("${repoOwnerName}", pullRequest.repoOwnerName())
                .replace("${repoName}", pullRequest.repoName()).replace("${pullRequestNumber}", String.valueOf(pullRequest.number));
        GraphqlRepositoryResponse repository = post(UrlUtils.addRelativePaths(apiUrl, "graphql"), GraphqlRepositoryResponse.class, request);
        return repository.data.repository.pullRequest.reviewThreads.nodes;
    }


    public PullRequest getPullRequest(String ownerName, String repoName, long pullNumber) {
        return get(pullRequestUrl(ownerName, repoName, pullNumber), PullRequest.class);
    }

    public void mergePullRequest(PullRequest pullRequest) {
        setupAuthenticatedConnection();
        PullMergeRequest pullMergeRequest = new PullMergeRequest();
        pullMergeRequest.mergeMethod = "merge";
        post(pullRequestUrl(pullRequest) + "/merge", PullMergeRequest.class, pullMergeRequest);
    }

    public PullRequest updatePullRequest(PullRequestForUpdate pullRequest) {
        setupAuthenticatedConnection();
        return patch(pullRequestUrl(pullRequest.repoOwner, pullRequest.repoName, pullRequest.number), PullRequest.class,
                pullRequest, Collections.emptyList());
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
        get(UrlUtils.addRelativePaths(apiUrl,"user"), String.class);
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
        return pullRequestUrl(pullRequest.repoOwnerName(), pullRequest.repoName(), pullRequest.number);
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
