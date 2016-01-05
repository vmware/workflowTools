package com.vmware;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugResolutionType;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests bugzilla api.
 */
public class TestBugzillaApi extends BaseTests {
    private static Bugzilla bugzilla;

    private static String bugzillaUsername;

    private static final int TEST_BUG_ID = 1567574;

    @BeforeClass
    public static void setupBugzilla() throws IllegalAccessException, IOException, URISyntaxException {
        bugzillaUsername = testProperties.getProperty("bugzilla.username");
        String bugzillaUrl = testProperties.getProperty("bugzilla.url");
        bugzilla = new Bugzilla(bugzillaUrl, bugzillaUsername, 1001);
        bugzilla.setupAuthenticatedConnection();
    }


    @Test
    public void canGetBug() throws IOException, URISyntaxException, IllegalAccessException {
        Bug bugInfo = bugzilla.getBugById(TEST_BUG_ID);
        assertNotNull(bugInfo);
        assertEquals(String.valueOf(TEST_BUG_ID), bugInfo.getKey());
    }

    @Test
    public void canResolveBug() throws IllegalAccessException, IOException, URISyntaxException {
        bugzilla.resolveBug(TEST_BUG_ID, BugResolutionType.WontFix);
    }

    @Test
    public void canAddBugComment() throws IOException {
        bugzilla.addBugComment(TEST_BUG_ID, "Test comment");
    }

    @Test
    public void canGetAssignedBugs() throws IOException, URISyntaxException, XmlRpcException {
        List<Bug> bugsList = bugzilla.getBugsForQuery("M31");
        assertTrue(bugsList.size() > 0);
    }
}
