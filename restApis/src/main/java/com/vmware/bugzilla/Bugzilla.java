package com.vmware.bugzilla;

import com.vmware.AbstractService;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.credentials.UsernamePasswordAsker;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.xmlrpc.CookieAwareXmlRpcClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.vmware.rest.cookie.ApiAuthentication.bugzilla_cookie;


/**
 * Used for http calls against Bugzilla. Has to support bugzilla 3 version so can't use a REST API.
 * That's what you get for customizing Bugzilla, a brain dead decision.
 */
public class Bugzilla extends AbstractService {

    CookieAwareXmlRpcClient xmlRpcClient;
    private int testBugNumber;

    public Bugzilla(String bugzillaUrl, String username, int testBugNumber) throws IOException, URISyntaxException {
        super(bugzillaUrl, "xmlrpc.cgi", ApiAuthentication.bugzilla_cookie, username);
        this.testBugNumber = testBugNumber;
        System.setProperty("jsse.enableSNIExtension", "false");
        xmlRpcClient = new CookieAwareXmlRpcClient(new URL(apiUrl));
    }

    public List<Bug> getBugsForQuery(String savedQueryToRun) throws IOException {
        Map values = xmlRpcClient.executeCall("Search.run_saved_query", username, savedQueryToRun);
        Object[] bugs = (Object[]) values.get("bugs");
        List<Bug> bugList = new ArrayList<>();
        for (Object bug : bugs) {
            bugList.add(new Bug((Map) bug));
        }
        return bugList;
    }

    public Bug getBugById(int id) throws IOException {
        Map values = xmlRpcClient.executeCall("Bug.show_bug", id);
        return new Bug(values);
    }

    public Bug getBugByIdWithoutException(int id) throws IOException {
        try {
            return getBugById(id);
        } catch (NotFoundException nfe) {
            return new Bug(String.valueOf(id));
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

    @Override
    protected void checkAuthenticationAgainstServer() throws IOException, URISyntaxException {
        getBugById(testBugNumber);

    }

    public void login() throws IllegalAccessException, IOException, URISyntaxException {
        loginManually();
    }

    @Override
    protected void loginManually() throws IllegalAccessException, IOException, URISyntaxException {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(bugzilla_cookie);

        Map result = xmlRpcClient.executeCall("User.login", credentials.toBugzillaLogin());
        Integer sessionId = (Integer) result.get("id");
        log.info(String.valueOf(sessionId));
    }

}
