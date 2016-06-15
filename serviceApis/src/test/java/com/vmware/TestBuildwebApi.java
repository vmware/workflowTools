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
        String username = testProperties.getProperty("buildweb.username");
        buildweb = new Buildweb(url, username);
    }

    @Test
    public void canGetSandboxBuild() {
        SandboxBuild build = buildweb.getSandboxBuild("7292096");
        assertEquals(7292096, build.id);
        assertEquals(BuildResult.SUCCESS, build.getBuildResult());
    }

    @Test
    public void buildWithCompileErrorStateIsTreatedAsFailed() {
        SandboxBuild build = buildweb.getSandboxBuild("7202366");
        assertEquals(7202366, build.id);
        assertEquals(BuildResult.FAILURE, build.getBuildResult());
    }


}
