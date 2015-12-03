package com.vmware.jira;

import com.vmware.AbstractRestService;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueTimeTracking;
import com.vmware.jira.domain.IssuesResponse;
import com.vmware.jira.domain.IssueTransition;
import com.vmware.jira.domain.IssueTransitions;
import com.vmware.jira.domain.IssueUpdate;
import com.vmware.jira.domain.LoginInfo;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.MenuSection;
import com.vmware.jira.domain.MenuSections;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.rest.SslUtils;
import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.json.NumericalEnum;
import com.vmware.rest.RestConnection;
import com.vmware.rest.request.UrlParam;
import com.vmware.rest.credentials.UsernamePasswordAsker;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.request.RequestBodyHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;
import static com.vmware.jira.domain.IssueTypeDefinition.Bug;
import static com.vmware.jira.domain.IssueTypeDefinition.Feature;
import static com.vmware.jira.domain.IssueTypeDefinition.Improvement;
import static com.vmware.jira.domain.IssueTypeDefinition.TechComm;
import static com.vmware.rest.cookie.ApiAuthentication.jira;

public class Jira extends AbstractRestService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private String loginUrl;
    private String searchUrl;
    private String legacyApiUrl;
    private String greenhopperUrl;

    public Jira(String jiraUrl) throws IOException, URISyntaxException, IllegalAccessException {
        super(jiraUrl, "rest/api/2/", ApiAuthentication.jira, null);
        this.connection = new RestConnection(RequestBodyHandling.AsStringJsonEntity);
        this.loginUrl = baseUrl + "login.jsp";
        this.searchUrl = apiUrl + "search";
        this.legacyApiUrl = baseUrl + "rest/api/1.0/";
        this.greenhopperUrl = baseUrl + "rest/greenhopper/1.0/";
    }

    public List<MenuItem> getRecentBoardItems() throws IOException, URISyntaxException {
        List<MenuItem> recentItems = new ArrayList<MenuItem>();
        String url = legacyApiUrl + "menus/greenhopper_menu?inAdminMode=false";
        MenuSection[] sections = connection.get(url, MenuSections.class).sections;
        if (sections.length == 0) {
            return recentItems;
        }

        for (MenuItem menuItem : sections[0].items) {
            if (menuItem.isRealItem()) {
                recentItems.add(menuItem);
            }
        }

        return recentItems;
    }

    public RapidView getRapidView(String viewId) throws IOException, URISyntaxException {
        String url = greenhopperUrl + "xboard/plan/backlog/data.json";
        RapidView rapidView = connection.get(url, RapidView.class, new UrlParam("rapidViewId", viewId));
        return rapidView;
    }

    public Issue getIssueByKey(String key) throws IOException, URISyntaxException {
        return connection.get(urlBaseForKey(key), Issue.class);
    }

    public IssuesResponse searchForIssues(SearchRequest searchRequest) throws IllegalAccessException, IOException, URISyntaxException {
        return connection.post(searchUrl, IssuesResponse.class, searchRequest);
    }

    public IssuesResponse getOpenTasksForUser(String username) throws IOException, URISyntaxException {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND assignee in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(username));
        return connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
    }

    public IssuesResponse getCreatedTasksForUser(String username) throws IOException, URISyntaxException {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND reporter in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(username));
        return connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
    }

    public IssueTransitions getAllowedTransitions(String key) throws IOException, URISyntaxException {
        IssueTransitions transitions = connection.get(urlBaseForKey(key) + "transitions", IssueTransitions.class);
        if (transitions == null) {
            transitions = new IssueTransitions();
        }
        transitions.issueKey = key;
        return transitions;
    }

    public void transitionIssue(IssueTransition transition) throws IOException, URISyntaxException, IllegalAccessException {
        IssueUpdate updateIssue = new IssueUpdate(transition);
        connection.post(urlBaseForKey(transition.issueId) + "transitions", updateIssue);
    }

    public Issue createIssue(Issue issue) throws IllegalAccessException, IOException, URISyntaxException {
        return connection.post(apiUrl + "issue", Issue.class, issue);
    }

    public void updateIssue(Issue issue) throws IllegalAccessException, IOException, URISyntaxException {
        connection.put(urlBaseForKey(issue.getKey()), issue);
    }

    public void updateIssueEstimate(String key, int estimateInHours) throws IllegalAccessException, IOException, URISyntaxException {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.timetracking = new IssueTimeTracking(estimateInHours + "h");
        connection.put(urlBaseForKey(key), updateIssue);
    }

    public void updateIssueStoryPointsOnly(Issue issue) throws IllegalAccessException, IOException, URISyntaxException {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.storyPoints = issue.fields.storyPoints;
        connection.put(urlBaseForKey(issue.getKey()), updateIssue);
    }

    public void deleteIssue(String key) throws IllegalAccessException, IOException, URISyntaxException {
        connection.delete(urlBaseForKey(key));
    }

    @Override
    protected void loginManually() throws IllegalAccessException, IOException, URISyntaxException {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(jira);
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        connection.post(loginUrl, new LoginInfo(credentials));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);
    }

    @Override
    protected void checkAuthenticationAgainstServer() throws IOException, URISyntaxException {
        connection.get(apiUrl + "issue/HW-1001/editmeta",null);
        if (!connection.hasCookie(jira)) {
            log.warn("Cookie {} should have been retrieved from jira login!", jira.getCookieName());
        }
    }

    public String urlBaseForKey(String key) {
        return apiUrl + "issue/" + key + "/";
    }

    private String generateNumericalEnumListAsInts(NumericalEnum... numericalEnums) {
        String statusText = "";
        for (NumericalEnum enumValue : numericalEnums) {
            if (!statusText.isEmpty()) {
                statusText += ",";
            }
            statusText += enumValue.getCode();
        }
        return statusText;
    }

    private String escapeUsername(String username) {
        return username.replace(".", "\\\\u002e");
    }


}
