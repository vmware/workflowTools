package com.vmware.bugzilla;

import com.vmware.AbstractService;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.rest.HttpConnection;
import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.credentials.UsernamePasswordAsker;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.InternalServerException;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.rest.request.RequestBodyHandling;
import com.vmware.xmlrpc.CookieAwareXmlRpcClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.vmware.rest.cookie.ApiAuthentication.bugzilla_cookie;
import static java.lang.String.format;


/**
 * Used for http calls against Bugzilla. Has to support bugzilla 3 version so can't use a REST API.
 * That's what you get for customizing Bugzilla, a brain dead decision.
 */
public class Bugzilla extends AbstractService {

    private final HttpConnection connection;
    private CookieAwareXmlRpcClient xmlRpcClient;
    private int testBugNumber;

    public Bugzilla(String bugzillaUrl, String username, int testBugNumber) throws IOException, URISyntaxException {
        super(bugzillaUrl, "xmlrpc.cgi", ApiAuthentication.bugzilla_cookie, username);
        this.testBugNumber = testBugNumber;
        System.setProperty("jsse.enableSNIExtension", "false");
        xmlRpcClient = new CookieAwareXmlRpcClient(new URL(apiUrl));
        connection = new HttpConnection(RequestBodyHandling.AsMultiPartFormEntity);
    }

    public List<Bug> getBugsForQuery(String savedQueryToRun) throws IOException {
        Map values = xmlRpcClient.executeCall("Search.run_saved_query", username, savedQueryToRun);
        Object[] bugs = (Object[]) values.get("bugs");
        List<Bug> bugList = new ArrayList<>();
        for (Object bug : bugs) {
            Map bugValues = (Map) bug;
            bugValues.put("web_url", constructFullBugUrl((Integer) bugValues.get("bug_id")));
            bugList.add(new Bug((Map) bug));
        }
        return bugList;
    }

    public Bug getBugById(int id) throws IOException {
        Map values = xmlRpcClient.executeCall("Bug.show_bug", id);
        values.put("web_url", constructFullBugUrl(id));
        return new Bug(values);
    }

    public Bug getBugByIdWithoutException(int id) throws IOException {
        try {
            return getBugById(id);
        } catch (NotFoundException nfe) {
            return new Bug(id);
        }
    }

    public List<String> getSavedQueries() throws IOException {
        Object[] values = xmlRpcClient.executeCall("Search.get_all_saved_queries", username);
        List<String> queries = new ArrayList<>();
        for (Object value : values) {
            queries.add(String.valueOf(value));
        }
        return queries;
    }

    public void resolveBug(int bugId, BugResolutionType resolution) throws IllegalAccessException, IOException, URISyntaxException {
        Bug bugToResolve = getBugById(bugId);
        if (bugToResolve.resolution == resolution) {
            log.info("Bug with id {} already has resolution {}", bugId, resolution);
            return;
        }
        bugToResolve.knob = "resolve";
        bugToResolve.resolution = resolution;
        bugToResolve.resolutionComment = "resolved";
        bugToResolve.resolveCc = "dbiggs";
        String response = connection.post(baseUrl + "process_bug.cgi", String.class, bugToResolve);

        Bug updatedBug = getBugById(bugId);
        if (updatedBug.resolution != resolution) {
            throw new InternalServerException(
                    format("Bug %s resolution was %s expected it to be %s after update\n%s", bugId, updatedBug.resolution, resolution, response));
        }
    }

    public void addBugComment(int bugId, String comment) throws IOException {
        SimpleDateFormat commentDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        xmlRpcClient.executeCall("Bug.add_comment", bugId, comment, commentDateFormat.format(new Date()), 1);
    }

    public String constructFullBugUrl(int bugNumber) {
        return baseUrl + "show_bug.cgi?id=" + bugNumber;
    }

    @Override
    protected void checkAuthenticationAgainstServer() throws IOException, URISyntaxException {
        getBugById(testBugNumber);

    }

    @Override
    protected void loginManually() throws IllegalAccessException, IOException, URISyntaxException {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(bugzilla_cookie);

        Map result = xmlRpcClient.executeCall("User.login", credentials.toBugzillaLogin());
        Integer sessionId = (Integer) result.get("id");
        log.info(String.valueOf(sessionId));
    }

}
