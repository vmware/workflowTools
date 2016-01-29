package com.vmware.bugzilla;

import com.vmware.AbstractService;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugKnobType;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.InternalServerException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.xmlrpc.CookieAwareXmlRpcClient;
import com.vmware.xmlrpc.MapObjectConverter;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;


/**
 * Used for http calls against Bugzilla. Has to support bugzilla 3 version so can't use a REST API.
 * That's what you get for customizing Bugzilla, a brain dead decision.
 */
public class Bugzilla extends AbstractService {

    private final HttpConnection connection;
    private CookieAwareXmlRpcClient xmlRpcClient;
    private MapObjectConverter mapConverter;
    private int testBugNumber;

    public Bugzilla(String bugzillaUrl, String username, int testBugNumber) {
        super(bugzillaUrl, "xmlrpc.cgi", ApiAuthentication.bugzilla_cookie, username);
        this.testBugNumber = testBugNumber;
        System.setProperty("jsse.enableSNIExtension", "false");
        xmlRpcClient = new CookieAwareXmlRpcClient(apiUrl);
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);
        mapConverter = new MapObjectConverter();
    }

    public List<Bug> getBugsForQuery(String savedQueryToRun) {
        Map values = xmlRpcClient.executeCall("Search.run_saved_query", username, savedQueryToRun);
        Object[] bugs = (Object[]) values.get("bugs");
        List<Bug> bugList = new ArrayList<>();
        for (Object bug : bugs) {
            Map bugValues = (Map) bug;
            bugValues.put("web_url", constructFullBugUrl((Integer) bugValues.get("bug_id")));
            bugList.add(mapConverter.fromMap(bugValues, Bug.class));
        }
        return bugList;
    }

    public Bug getBugById(int id) {
        Map values = xmlRpcClient.executeCall("Bug.show_bug", id);
        values.put("web_url", constructFullBugUrl(id));
        return mapConverter.fromMap(values, Bug.class);
    }

    public Bug getBugByIdWithoutException(int id) {
        try {
            return getBugById(id);
        } catch (NotFoundException nfe) {
            return new Bug(id);
        }
    }

    public List<String> getSavedQueries() {
        Object[] values = xmlRpcClient.executeCall("Search.get_all_saved_queries", username);
        List<String> queries = new ArrayList<>();
        for (Object value : values) {
            queries.add(String.valueOf(value));
        }
        log.debug("Bugzilla queries for user {}, {}", username, queries.toString());
        return queries;
    }

    public boolean containsSavedQuery(String queryName) {
        List<String> savedQueries = getSavedQueries();
        return savedQueries.contains(queryName);
    }

    public void resolveBug(int bugId, BugResolutionType resolution) {
        Bug bugToResolve = getBugById(bugId);
        if (bugToResolve.resolution == resolution) {
            log.info("Bug with id {} already has resolution {}", bugId, resolution);
            return;
        }
        bugToResolve.knob = BugKnobType.resolve;
        bugToResolve.changed = 1;
        bugToResolve.resolution = resolution;
        String response = connection.post(baseUrl + "process_bug.cgi", String.class, bugToResolve);

        Bug updatedBug = getBugById(bugId);
        if (updatedBug.resolution != resolution) {
            throw new InternalServerException(
                    format("Bug %s resolution was %s expected it to be %s after update\n%s", bugId, updatedBug.resolution, resolution, response));
        }
        log.info("Resolved bug {} with resolution {}", bugId, resolution.getValue());
    }

    public void addBugComment(int bugId, String comment) {
        SimpleDateFormat commentDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        xmlRpcClient.executeCall("Bug.add_comment", bugId, comment, commentDateFormat.format(new Date()), 1);
    }

    public String constructFullBugUrl(int bugNumber) {
        return baseUrl + "show_bug.cgi?id=" + bugNumber;
    }

    @Override
    public boolean isBaseUriTrusted() {
        return xmlRpcClient.isUriTrusted(URI.create(baseUrl));
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        getBugById(testBugNumber);

    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType);

        Map result = xmlRpcClient.executeCall("User.login", credentials.toBugzillaLogin());
        Integer sessionId = (Integer) result.get("id");
        log.info(String.valueOf(sessionId));
    }

}
