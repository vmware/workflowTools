package com.vmware;

import com.vmware.github.Github;
import com.vmware.github.domain.ReleaseAsset;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestGitHubApi {

    @Test
    public void getReleaseAsset() throws IOException {
        Github github = new Github("https://api.github.com", "damienbiggs");
        ReleaseAsset[] assets = github.getReleaseAssets("repos/vmware/workflowTools/releases/43387689");
        assertEquals("workflowTools.jar", assets[0].name);

    }
}
