package com.vmware.gitlab;

import java.util.Collections;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.gitlab.domain.MergeAcceptRequest;
import com.vmware.gitlab.domain.MergeRequest;
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
                new ConfiguredGsonBuilder().namingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        String apiToken = readExistingApiToken(ApiAuthentication.gitlab);
        if (StringUtils.isNotBlank(apiToken)) {
            connection.addStatefulParam(new RequestHeader(PRIVATE_TOKEN_HEADER, apiToken));
        }
    }

    public MergeRequest[] getMergeRequests(String state) {
        return optimisticGet(apiUrl +"/merge_requests?state=" + state, MergeRequest[].class);
    }

    public MergeRequest createMergeRequest(MergeRequest mergeRequest) {
        return optimisticPost(mergeRequestsUrl(mergeRequest.targetProjectId), MergeRequest.class, mergeRequest);
    }

    public MergeRequest getMergeRequest(int projectId, int mergeRequestId) {
        return optimisticGet(mergeRequestUrl(projectId, mergeRequestId), MergeRequest.class);
    }

    public void approveMergeRequest(int projectId, int mergeRequestId) {
        try {
            optimisticPost(mergeRequestUrl(projectId, mergeRequestId) + "/approve", null, null,
                    Collections.singletonList(NotAuthorizedException.class));
        } catch (NotAuthorizedException nae) {
            throw new FatalException(nae, "Not authorized to approve merge request {} for project {}", mergeRequestId, projectId);
        }

    }

    public void acceptMergeRequest(MergeRequest mergeRequest) {
        try {
            MergeAcceptRequest acceptRequest = new MergeAcceptRequest(mergeRequest);
            String response = optimisticPut(mergeRequestUrl(mergeRequest.projectId, mergeRequest.iid) + "/merge", String.class, acceptRequest,
                    Collections.singletonList(NotAuthorizedException.class));
            log.debug(response);
        } catch (NotAuthorizedException nae) {
            throw new FatalException(nae, "Not authorized to accept merge request {} for project {}", mergeRequest.iid, mergeRequest.projectId);
        }
    }

    public void rebaseMergeRequest(int projectId, int mergeRequestId) {
        try {
            String response = optimisticPut(mergeRequestUrl(projectId, mergeRequestId) + "/rebase", String.class, null,
                    Collections.singletonList(ForbiddenException.class));
            log.debug("Rebase response: {}", response);
        } catch (ForbiddenException fe) {
            throw new FatalException(fe, "Not authorized to accept merge request {} for project {}", mergeRequestId, projectId);
        }
    }

    public MergeRequest updateMergeRequest(MergeRequest mergeRequest) {
        return optimisticPut(mergeRequestUrl(mergeRequest.projectId, mergeRequest.iid), MergeRequest.class, mergeRequest);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        optimisticGet(apiUrl + "/version", String.class);
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

    private String mergeRequestsUrl(int projectId) {
        return apiUrl + "/projects/" + projectId + "/merge_requests";
    }

    private String mergeRequestUrl(int projectId, int requestId) {
        return mergeRequestsUrl(projectId) + "/" + requestId;
    }


}
