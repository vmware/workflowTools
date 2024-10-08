package com.vmware;

import com.google.gson.Gson;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.ClasspathResource;
import com.vmware.util.IOUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public static void setupBugzilla() {
        bugzillaUsername = testProperties.getProperty("bugzilla.username");
        String bugzillaUrl = testProperties.getProperty("bugzilla.url");
        bugzilla = new Bugzilla(bugzillaUrl, bugzillaUsername, 1001, false, null, null);
        //bugzilla.setupAuthenticatedConnection();
    }

    @Test
    public void isUriTrusted() {
        assertTrue(bugzilla.isBaseUriTrusted());
    }

    @Test
    public void canGetBug() {
        Bug bugInfo = bugzilla.getBugById(TEST_BUG_ID);
        assertNotNull(bugInfo);
        assertEquals(String.valueOf(TEST_BUG_ID), bugInfo.getKey());
    }

    @Test
    public void canResolveBug() {
        bugzilla.resolveBug(TEST_BUG_ID, BugResolutionType.WontFix);
    }

    @Test
    public void canAddBugComment() {
        bugzilla.addBugComment(TEST_BUG_ID, "Test comment");
    }

    @Test
    public void canGetAssignedBugs() {
        List<Bug> bugsList = bugzilla.getBugsForQuery("M31");
        assertTrue(bugsList.size() > 0);
    }

    @Test
    public void canParseCookieText() {
        String cookieText = "Bugzilla_login=18180; Bugzilla_loginname=dbiggs; Bugzilla_logincookie=UR8QeZ3C9w; headerPinned=no; _pk_id.154.bc5f=707f1f93eb52492c.1727464655.";
        String[] cookies = cookieText.split(";");
        List<Cookie> parsedCookies = Arrays.stream(cookies).map(cookie -> new Cookie(URI.create(bugzilla.getBaseUrl()).getHost(), cookie, "/")).collect(Collectors.toList());
        assertEquals(5, parsedCookies.size());
        assertEquals("18180", parsedCookies.get(0).getValue());
        assertEquals("UR8QeZ3C9w", parsedCookies.get(2).getValue());
    }

    // more for future proofing, check that a bug could be properly deserialized from json
    // if the switch to the bugzilla rest api ever happens
    @Test
    public void canDeserializeBugFromJson() {
        String bugJsonText = new ClasspathResource("/bugAsJson.json", this.getClass()).getText();
        Gson gson = new ConfiguredGsonBuilder().build();
        Bug deserializedBug = gson.fromJson(bugJsonText, Bug.class);
        assertFalse("Bug should be found", deserializedBug.isNotFound());
        assertTrue("Description length should be greater than zero", deserializedBug.descriptionLength > 0);
    }
}
