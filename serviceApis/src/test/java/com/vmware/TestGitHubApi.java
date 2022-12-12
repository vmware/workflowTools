package com.vmware;

import com.vmware.github.Github;
import com.vmware.github.domain.ReleaseAsset;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class TestGitHubApi {

    @Test
    public void getReleaseAsset() throws IOException {
        Github github = new Github("https://api.github.com", "damienbiggs");
        ReleaseAsset asset = github.getReleaseAsset("repos/vmware/workflowTools/releases/assets/84876705");
        assertEquals("workflowTools.jar", asset.name);

    }
}
