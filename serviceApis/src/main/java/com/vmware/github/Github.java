package com.vmware.github;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.AutocompleteUser;
import com.vmware.github.domain.GraphqlRequest;
import com.vmware.github.domain.PullMergeResult;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.github.domain.PullMergeRequest;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.PullRequestUpdateBranchRequest;
import com.vmware.github.domain.PullRequestUpdateBranchResponse;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.RequestedReviewers;
import com.vmware.github.domain.Review;
import com.vmware.github.domain.ReviewThread;
import com.vmware.github.domain.Team;
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
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.xmlrpc.MapObjectConverter;

public class Github extends AbstractRestService {

    private final String graphqlUrl;

    public Github(String baseUrl, String graphqlUrl, String username) {
        super(baseUrl, "", ApiAuthentication.github_token, username);
        this.graphqlUrl = graphqlUrl;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
                        .namingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        String apiToken = readExistingApiToken(ApiAuthentication.github_token);
        if (StringUtils.isNotBlank(apiToken)) {
            connection.addStatefulParam(RequestHeader.aBearerAuthHeader(apiToken));
        }
    }

    public List<User> searchUsers(String companyName, String query) {
        String searchUsersQuery = new ClasspathResource("/githubSearchUsersGraphql.txt", this.getClass()).getText();
        GraphqlRequest request = new GraphqlRequest();

        request.query = searchUsersQuery.replace("${query}", query).replace("${companyName}", companyName);
        GraphqlResponse response = post(graphqlUrl, GraphqlResponse.class, request);
        return response.data.search.usersForCompany(companyName);
    }

    public PullRequest createPullRequest(PullRequestForUpdate pullRequest) {
        setupAuthenticatedConnection();
        return post(pullRequestsUrl(pullRequest.repoOwner, pullRequest.repoName), PullRequest.class, pullRequest);
    }

    public PullRequestUpdateBranchResponse updatePullRequestBranch(PullRequest pullRequest) {
        PullRequestUpdateBranchRequest request = new PullRequestUpdateBranchRequest();
        request.expectedHeadSha = pullRequest.head.sha;
        return put(pullRequestUrl(pullRequest) + "/update-branch", PullRequestUpdateBranchResponse.class, request);
    }

    public Optional<PullRequest> getPullRequestForSourceBranch(String ownerName, String repoName, String sourceBranch) {
        PullRequest[] pullRequests = get(pullRequestsUrl(ownerName, repoName), PullRequest[].class,
                new UrlParam("head", ownerName + ":" + sourceBranch));
        return Optional.ofNullable(pullRequests.length > 0 ? pullRequests[0] : null);
    }

    public List<Review> getApprovedReviewsForPullRequest(PullRequest pullRequest) {
        return Arrays.stream(getReviewsForPullRequest(pullRequest)).filter(Review::isApproved).collect(Collectors.toList());
    }

    public Review[] getReviewsForPullRequest(PullRequest pullRequest) {
        return get(pullRequestUrl(pullRequest) + "/reviews", Review[].class);
    }

    public GraphqlResponse.PullRequestNode getPullRequestViaGraphql(PullRequest pullRequest) {
        String reviewThreadsQuery = new ClasspathResource("/githubPullRequestGraphql.txt", this.getClass()).getText();
        GraphqlRequest request = new GraphqlRequest();

        request.query = reviewThreadsQuery.replace("${repoOwnerName}", pullRequest.repoOwnerName())
                .replace("${repoName}", pullRequest.repoName()).replace("${pullRequestNumber}", String.valueOf(pullRequest.number));
        GraphqlResponse repository = post(graphqlUrl, GraphqlResponse.class, request);
        return repository.data.repository.pullRequest;
    }


    public PullRequest getPullRequest(String ownerName, String repoName, long pullNumber) {
        return get(pullRequestUrl(ownerName, repoName, pullNumber), PullRequest.class);
    }

    public void mergePullRequest(PullRequest pullRequest, String mergeMethod, String commitTitle, String commitMessage) {
        setupAuthenticatedConnection();
        PullMergeRequest pullMergeRequest = new PullMergeRequest();
        pullMergeRequest.mergeMethod = mergeMethod;
        pullMergeRequest.commitTitle = commitTitle;
        pullMergeRequest.commitMessage = commitMessage;
        pullMergeRequest.sha = pullRequest.head.sha;
        PullMergeResult result = put(pullRequestUrl(pullRequest) + "/merge", PullMergeResult.class, pullMergeRequest);
        log.debug("Merge result: {} Sha: {}", result.message, result.sha);
        if (!result.merged) {
            throw new FatalException("Failed to merge pull request {}. Message: {}", pullRequest.number, result.message);
        }
    }

    public PullRequest updatePullRequest(PullRequestForUpdate pullRequest) {
        setupAuthenticatedConnection();
        return patch(pullRequestUrl(pullRequest.repoOwner, pullRequest.repoName, pullRequest.number), PullRequest.class,
                pullRequest, Collections.emptyList());
    }

    public void updatePullRequestDraftState(PullRequest pullRequest, boolean isDraft) {
        log.info("Marking pull request {} draft status as {}", pullRequest.number, isDraft);
        String graphqlText = new ClasspathResource("/githubDraftPullRequestGraphql.txt", this.getClass()).getText();
        String mutationName = isDraft ? "convertPullRequestToDraft" : "markPullRequestReadyForReview";
        GraphqlRequest request = new GraphqlRequest();

        request.query = graphqlText.replace("${mutationName}", mutationName).replace("${pullRequestId}", pullRequest.nodeId);
        Map<String, Map<String, Map<String, Map<String, Object>>>> response = post(graphqlUrl, Map.class, request);

        GraphqlResponse.PullRequestNode updatedPullRequest = new MapObjectConverter()
                .fromMap(response.get("data").get(mutationName).get("pullRequest"), GraphqlResponse.PullRequestNode.class);
        if (updatedPullRequest.number != pullRequest.number) {
            throw new FatalException("Wrong pull request {} was updated", updatedPullRequest.number);
        }
        if (updatedPullRequest.isDraft != isDraft) {
            throw new FatalException("Pull request {} draft status was not set to {}", updatedPullRequest.number, isDraft);
        }
    }

    public void addReviewersToPullRequest(PullRequest pullRequest, Set<AutocompleteUser> users) {
        if (users.isEmpty()) {
            return;
        }
        RequestedReviewers requestedReviewers = new RequestedReviewers();
        requestedReviewers.reviewers = users.stream().filter(user -> user instanceof User)
                .map(user -> ((User) user).login).toArray(String[]::new);
        requestedReviewers.teamReviewers = users.stream().filter(team -> team instanceof Team)
                .map(team -> ((Team) team).slug).toArray(String[]::new);
        post(pullRequestUrl(pullRequest) + "/requested_reviewers", String.class, requestedReviewers);
    }

    public void removeReviewersFromPullRequest(PullRequest pullRequest, Set<AutocompleteUser> users) {
        if (users.isEmpty()) {
            return;
        }
        RequestedReviewers requestedReviewers = new RequestedReviewers();
        requestedReviewers.reviewers = users.stream().filter(user -> user instanceof User)
                .map(user -> ((User) user).login).toArray(String[]::new);
        requestedReviewers.teamReviewers = users.stream().filter(team -> team instanceof Team)
                .map(team -> ((Team) team).slug).toArray(String[]::new);
        delete(pullRequestUrl(pullRequest) + "/requested_reviewers", requestedReviewers, Collections.emptyList());
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
        saveApiToken(privateToken, ApiAuthentication.github_token);
        connection.addStatefulParam(RequestHeader.aBearerAuthHeader(privateToken));
    }

    @Override
    protected File determineApiTokenFile(ApiAuthentication apiAuthentication) {
        String homeFolder = System.getProperty("user.home");
        File apiHostTokenFile = new File(homeFolder + File.separator +
                "." + URI.create(baseUrl).getHost() + "-" + apiAuthentication.getFileName().substring(1));
        if (apiHostTokenFile.exists()) {
            return apiHostTokenFile;
        } else {
            log.debug("Api token file {} does not exist", apiHostTokenFile.getPath());
        }
        return super.determineApiTokenFile(apiAuthentication);
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
