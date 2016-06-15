package com.vmware.buildweb;

import com.vmware.AbstractRestBuildService;
import com.vmware.AbstractRestService;
import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.buildweb.domain.SandboxBuild;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

import java.util.List;

/**
 * VMware specific build service.
 */
public class Buildweb extends AbstractRestBuildService {

    public Buildweb(String buildwebUrl, String username) {
        super(buildwebUrl, "/", ApiAuthentication.none, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
    }

    public SandboxBuild getSandboxBuild(String id) {
        return connection.get(baseUrl + "sb/build/" + id, SandboxBuild.class);
    }



    @Override
    protected void checkAuthenticationAgainstServer() {
        log.info("No need to authenticate against Buildweb");
    }

    @Override
    protected void loginManually() {
    }

    @Override
    protected BuildResult getResultForBuild(String url) {
        SandboxBuild build = optimisticGet(url, SandboxBuild.class);
        return build.getBuildResult();
    }

    @Override
    protected void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result) {
        draft.buildwebBuildsAreSuccessful = result;
    }
}
