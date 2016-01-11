package com.vmware.jira;

import com.vmware.AbstractRestService;
import com.vmware.utils.enums.ComplexEnum;
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
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.UrlParam;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.request.RequestBodyHandling;
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
import static com.vmware.http.cookie.ApiAuthentication.jira;

public class Jira extends AbstractRestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String loginUrl;
    private String searchUrl;
    private String legacyApiUrl;
    private String greenhopperUrl;
    private String testIssueKey;

    public Jira(String jiraUrl, String testIssueKey) {
        super(jiraUrl, "rest/api/2/", ApiAuthentication.jira, null);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        this.loginUrl = baseUrl + "login.jsp";
        this.searchUrl = apiUrl + "search";
        this.legacyApiUrl = baseUrl + "rest/api/1.0/";
        this.greenhopperUrl = baseUrl + "rest/greenhopper/1.0/";
        this.testIssueKey = testIssueKey;
    }

    public List<MenuItem> getRecentBoardItems() {
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

    public RapidView getRapidView(String viewId) {
        String url = greenhopperUrl + "xboard/plan/backlog/data.json";
        RapidView rapidView = connection.get(url, RapidView.class, new UrlParam("rapidViewId", viewId));
        return rapidView;
    }

    public Issue getIssueByKey(String key) {
        return connection.get(urlBaseForKey(key), Issue.class);
    }

    public Issue getIssueWithoutException(String key) throws IOException, URISyntaxException {
        try {
            return getIssueByKey(key);
        } catch (NotFoundException e) {
            log.debug(e.getMessage(), e);
            return Issue.aNotFoundIssue(key);
        }
    }

    public IssuesResponse searchForIssues(SearchRequest searchRequest) {
        return connection.post(searchUrl, IssuesResponse.class, searchRequest);
    }

    public IssuesResponse getOpenTasksForUser(String username) {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND assignee in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(username));
        IssuesResponse response = connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
        log.debug("{} tasks found", response.issues.length);
        return response;
    }

    public IssuesResponse getCreatedTasksForUser(String username) {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND reporter in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(username));
        return connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
    }

    public IssueTransitions getAllowedTransitions(String key) {
        IssueTransitions transitions = connection.get(urlBaseForKey(key) + "transitions", IssueTransitions.class);
        if (transitions == null) {
            transitions = new IssueTransitions();
        }
        transitions.issueKey = key;
        return transitions;
    }

    public void transitionIssue(IssueTransition transition) {
        IssueUpdate updateIssue = new IssueUpdate(transition);
        connection.post(urlBaseForKey(transition.issueId) + "transitions", updateIssue);
    }

    public Issue createIssue(Issue issue) {
        return connection.post(apiUrl + "issue", Issue.class, issue);
    }

    public void updateIssue(Issue issue) {
        connection.put(urlBaseForKey(issue.getKey()), issue);
    }

    public void updateIssueEstimate(String key, int estimateInHours) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.timetracking = new IssueTimeTracking(estimateInHours + "h");
        connection.put(urlBaseForKey(key), updateIssue);
    }

    public void updateIssueStoryPointsOnly(Issue issue) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.storyPoints = issue.fields.storyPoints;
        connection.put(urlBaseForKey(issue.getKey()), updateIssue);
    }

    public void deleteIssue(String key) {
        connection.delete(urlBaseForKey(key));
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(jira);
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        connection.post(loginUrl, new LoginInfo(credentials));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        connection.get(apiUrl + "issue/" + testIssueKey + "/editmeta",null);
        if (!connection.hasCookie(jira)) {
            log.warn("Cookie {} should have been retrieved from jira login!", jira.getCookieName());
        }
    }

    public String urlBaseForKey(String key) {
        return apiUrl + "issue/" + key + "/";
    }

    private String generateNumericalEnumListAsInts(ComplexEnum... complexEnums) {
        String statusText = "";
        for (ComplexEnum enumValue : complexEnums) {
            if (!statusText.isEmpty()) {
                statusText += ",";
            }
            statusText += enumValue.getValue();
        }
        return statusText;
    }

    private String escapeUsername(String username) {
        return username.replace(".", "\\\\u002e");
    }


}
