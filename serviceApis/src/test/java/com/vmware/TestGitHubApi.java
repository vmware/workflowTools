package com.vmware;

import com.vmware.github.Github;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.ReleaseAsset;
import com.vmware.github.domain.ReviewThread;
import com.vmware.github.domain.User;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestGitHubApi {

    @Test
    public void getReleaseAsset() throws IOException {
        Github github = new Github("https://api.github.com", "damienbiggs");
        ReleaseAsset[] assets = github.getReleaseAssets("repos/vmware/workflowTools/releases/43387689");
        assertEquals("workflowTools.jar", assets[0].name);
    }

    @Test
    public void getReviewThreads() {
        Github github = new Github("https://api.github.com", "damienbiggs");
        github.setupAuthenticatedConnection();
        PullRequest pullRequest = github.getPullRequest("vmware", "workflowTools", 12);
        ReviewThread[] threads = github.getReviewThreadsForPullRequest(pullRequest);
        assertTrue(threads.length > 0);
    }

    @Test
    public void searchUsers() {
        Github github = new Github("https://api.github.com", "damienbiggs");
        github.setupAuthenticatedConnection();
        List<User> users = github.searchUsers("VMware", "damienbigg");
        assertEquals(1, users.size());
    }
}
