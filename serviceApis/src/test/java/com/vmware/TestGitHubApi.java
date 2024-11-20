package com.vmware;

import com.vmware.github.Github;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.ReviewThread;
import com.vmware.github.domain.User;
import com.vmware.xmlrpc.MapObjectConverter;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestGitHubApi {

    @Test
    public void testConversion() {
        Map<String, Object> response = new HashMap<>();
        response.put("number", 55D);
        response.put("isDraft", true);
        GraphqlResponse.PullRequestNode pullRequestNode = new MapObjectConverter().fromMap(response, GraphqlResponse.PullRequestNode.class);
        assertEquals(55d, pullRequestNode.number, 0d);
        assertTrue(pullRequestNode.isDraft);
    }

    @Test
    public void getPullRequest() {
        Github github = new Github("https://api.github.com", "https://api.github.com/graphql", "damienbiggs");
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 18);
        assertEquals(18, pullRequest.number);
    }

    @Test
    public void getReleaseAsset() throws IOException {
        Github github = new Github("https://api.github.com", "https://api.github.com/graphql", "damienbiggs");
        ReleaseAsset[] assets = github.getReleaseAssets("repos/vmware/workflowTools/releases/43387689");
        assertEquals("workflowTools.jar", assets[0].name);
    }

    @Test
    public void getReviewThreads() {
        Github github = new Github("https://api.github.com", "https://api.github.com/graphql", "damienbiggs");
        github.setupAuthenticatedConnection();
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 12);
        GraphqlResponse.PullRequestNode pullRequestNode = github.getPullRequestViaGraphql(pullRequest);
        assertTrue(pullRequestNode.reviewThreads.nodes.length > 0);
    }

    @Test
    public void searchUsers() {
        Github github = new Github("https://api.github.com", "https://api.github.com/graphql", "damienbiggs");
        github.setupAuthenticatedConnection();
        List<User> users = github.searchUsers("VMware", "damienbigg");
        assertEquals(1, users.size());
    }
}
