package com.vmware.gitlab;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.gitlab.domain.MergeAcceptRequest;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovalRule;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.gitlab.domain.MergeRequestCommitVersion;
import com.vmware.gitlab.domain.MergeRequestDiscussion;
import com.vmware.gitlab.domain.User;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.ForbiddenException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

public class Gitlab extends AbstractRestService {

    private static final String PRIVATE_TOKEN_HEADER = "Private-Token";

    public Gitlab(String baseUrl, String username) {
        super(baseUrl, "api/v4", ApiAuthentication.gitlab, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss.SSS")
                        .namingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        String apiToken = readExistingApiToken(ApiAuthentication.gitlab);
        if (StringUtils.isNotBlank(apiToken)) {
            connection.addStatefulParam(new RequestHeader(PRIVATE_TOKEN_HEADER, apiToken));
        }
    }

    public List<User> searchUsers(String query) {
        return Arrays.asList(get(apiUrl+ "/search?scope=users&search=" + StringUtils.urlEncode(query), User[].class));
    }

    public MergeRequest[] getMergeRequests(String state) {
        return get(apiUrl +"/merge_requests?state=" + state, MergeRequest[].class);
    }

    public MergeRequest createMergeRequest(MergeRequest mergeRequest) {
        return post(mergeRequestsUrl(mergeRequest.targetProjectId), MergeRequest.class, mergeRequest);
    }

    public MergeRequest getMergeRequest(int projectId, int mergeRequestId) {
        return get(mergeRequestUrl(projectId, mergeRequestId), MergeRequest.class);
    }

    public MergeRequestApprovals getMergeRequestApprovals(int projectId, int mergeRequestId) {
        return get(mergeRequestUrl(projectId, mergeRequestId) + "/approvals", MergeRequestApprovals.class);
    }

    public MergeRequestApprovalRule[] getMergeRequestApprovalRules(int projectId, int mergeRequestId) {
        return get(mergeRequestUrl(projectId, mergeRequestId) + "/approval_rules", MergeRequestApprovalRule[].class);
    }

    public Set<MergeRequestDiscussion> getOpenMergeRequestDiscussions(int projectId, int mergeRequestId) {
        MergeRequestDiscussion[] discussions = get(mergeRequestUrl(projectId, mergeRequestId) + "/discussions", MergeRequestDiscussion[].class);
        return Arrays.stream(discussions).filter(MergeRequestDiscussion::isOpenDiffDiscussion).collect(Collectors.toSet());
    }

    public MergeRequestCommitVersion[] getOpenMergeRequestCommitVersions(int projectId, int mergeRequestId) {
        return get(mergeRequestUrl(projectId, mergeRequestId) + "/versions", MergeRequestCommitVersion[].class);
    }

    public MergeRequestApprovalRule createMergeRequestApprovalRule(int projectId, int mergeRequestId, MergeRequestApprovalRule rule) {
        return post(mergeRequestUrl(projectId, mergeRequestId) + "/approval_rules", MergeRequestApprovalRule.class, rule);
    }

    public MergeRequestApprovalRule updateMergeRequestApprovalRule(int projectId, int mergeRequestId, MergeRequestApprovalRule rule) {
        return put(mergeRequestUrl(projectId, mergeRequestId) + "/approval_rules/" + rule.id, MergeRequestApprovalRule.class, rule);
    }

    public void deleteMergeRequestApprovalRule(int projectId, int mergeRequestId, long ruleId) {
        delete(mergeRequestUrl(projectId, mergeRequestId) + "/approval_rules/" + ruleId);
    }

    public MergeRequestApprovals approveMergeRequest(int projectId, int mergeRequestId) {
        try {
            return post(mergeRequestUrl(projectId, mergeRequestId) + "/approve", MergeRequestApprovals.class, null,
                    Collections.singletonList(NotAuthorizedException.class));
        } catch (NotAuthorizedException nae) {
            throw new FatalException(nae, "Not authorized to approve merge request {} for project {}", mergeRequestId, projectId);
        }

    }

    public void acceptMergeRequest(MergeRequest mergeRequest) {
        try {
            MergeAcceptRequest acceptRequest = new MergeAcceptRequest(mergeRequest);
            String response = put(mergeRequestUrl(mergeRequest.projectId, mergeRequest.iid) + "/merge", String.class, acceptRequest,
                    Collections.singletonList(NotAuthorizedException.class));
            log.debug(response);
        } catch (NotAuthorizedException nae) {
            throw new FatalException(nae, "Not authorized to accept merge request {} for project {}", mergeRequest.iid, mergeRequest.projectId);
        }
    }

    public void rebaseMergeRequest(int projectId, int mergeRequestId) {
        try {
            String response = put(mergeRequestUrl(projectId, mergeRequestId) + "/rebase", String.class, null,
                    Collections.singletonList(ForbiddenException.class));
            log.debug("Rebase response: {}", response);
        } catch (ForbiddenException fe) {
            throw new FatalException(fe, "Not authorized to accept merge request {} for project {}", mergeRequestId, projectId);
        }
    }

    public MergeRequest updateMergeRequest(MergeRequest mergeRequest) {
        return put(mergeRequestUrl(mergeRequest.projectId, mergeRequest.iid), MergeRequest.class, mergeRequest);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        get(apiUrl + "/version", String.class);
    }

    @Override
    protected void loginManually() {
        connection.removeStatefulParam(PRIVATE_TOKEN_HEADER);
        log.info("Gitlab uses personal access tokens for third party API access.");
        log.info("On the UI, go to Settings -> Access Tokens and create a new personal access token");
        String privateToken = InputUtils.readValueUntilNotBlank("Enter Personal Access Token");
        saveApiToken(privateToken, ApiAuthentication.gitlab);
        connection.addStatefulParam(new RequestHeader(PRIVATE_TOKEN_HEADER, privateToken));
    }

    private String projectUrl(int projectId) {
        return apiUrl + "/projects/" + projectId;
    }

    private String mergeRequestsUrl(int projectId) {
        return projectUrl(projectId) + "/merge_requests";
    }

    private String mergeRequestUrl(int projectId, int requestId) {
        return mergeRequestsUrl(projectId) + "/" + requestId;
    }


}
