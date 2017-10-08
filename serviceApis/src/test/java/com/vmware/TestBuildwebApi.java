package com.vmware;

import com.vmware.buildweb.Buildweb;
import com.vmware.buildweb.domain.SandboxBuild;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests Buildweb api calls. This is VMware specific.
 */
public class TestBuildwebApi extends BaseTests {

    private Buildweb buildweb;

    @Before
    public void init() {
        String url = testProperties.getProperty("buildweb.url");
        String apiUrl = testProperties.getProperty("buildweb.api.url");
        String logsUrlPattern = testProperties.getProperty("buildweb.logs.url.pattern");
        String username = testProperties.getProperty("buildweb.username");
        buildweb = new Buildweb(url, apiUrl, logsUrlPattern, username);
    }

    @Test
    public void canGetSandboxBuild() {
        SandboxBuild build = buildweb.getSandboxBuild("7292096");
        assertEquals(7292096, build.id);
        assertEquals(BuildResult.SUCCESS, build.getBuildResult());
    }

    @Test
    public void buildWithCompileErrorStateIsTreatedAsFailed() {
        SandboxBuild build = buildweb.getSandboxBuild("11330096");
        assertEquals(11330096, build.id);
        assertEquals(BuildResult.FAILURE, build.getBuildResult());
        String buildOutput = buildweb.getBuildOutput(String.valueOf(build.id), 300);
        System.out.println(buildOutput);
    }


}
