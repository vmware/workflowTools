package com.vmware.jira;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.http.request.UrlParam;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolution;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTimeTracking;
import com.vmware.jira.domain.IssueTransition;
import com.vmware.jira.domain.IssueTransitions;
import com.vmware.jira.domain.IssueUpdate;
import com.vmware.jira.domain.IssuesResponse;
import com.vmware.jira.domain.LoginInfo;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.MenuSection;
import com.vmware.jira.domain.MenuSections;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.trello.domain.StringValue;
import com.vmware.util.complexenum.ComplexEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.vmware.http.cookie.ApiAuthentication.jira;
import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;
import static com.vmware.config.jira.IssueTypeDefinition.Bug;
import static com.vmware.config.jira.IssueTypeDefinition.Feature;
import static com.vmware.config.jira.IssueTypeDefinition.Improvement;
import static com.vmware.config.jira.IssueTypeDefinition.TechComm;

public class Jira extends AbstractRestService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String loginUrl;
    private String searchUrl;
    private String legacyApiUrl;
    private String agileUrl;
    private String greenhopperUrl;

    public Jira(String jiraUrl, String username, Map<String, String> customFieldNames) {
        super(jiraUrl, "rest/api/2/", ApiAuthentication.jira, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity, new ConfiguredGsonBuilder(customFieldNames).build());
        this.loginUrl = baseUrl + "login.jsp";
        this.searchUrl = apiUrl + "search";
        this.legacyApiUrl = baseUrl + "rest/api/1.0/";
        this.agileUrl = baseUrl + "rest/agile/1.0/";
        this.greenhopperUrl = baseUrl + "rest/greenhopper/1.0/";
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

    public Issue getIssueWithoutException(String key) {
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

    public IssuesResponse getOpenTasksForUser() {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND assignee=%s",
                issueTypesToGet, allowedStatuses, escapeUsername(getUsername()));
        IssuesResponse response = connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
        log.debug("{} tasks found", response.issues.length);
        return response;
    }

    public IssuesResponse getIssuesForUser(IssueStatusDefinition status, IssueResolutionDefinition resolution) {
        String jql = String.format("status=%s AND resolution=%s AND assignee=%s",
                status.getValue(), resolution != null ? resolution.getValue() : null, escapeUsername(getUsername()));
        IssuesResponse response = connection.get(searchUrl, IssuesResponse.class, new UrlParam("jql", jql));
        log.debug("{} tasks found", response.issues.length);
        return response;
    }

    public IssuesResponse getCreatedTasksForUser() {
        String allowedStatuses = generateNumericalEnumListAsInts(Open, Reopened, InProgress, InReview);
        String issueTypesToGet = generateNumericalEnumListAsInts(Improvement, Feature, Bug, TechComm);

        String jql = String.format("issuetype in (%s,subTaskIssueTypes()) AND status in (%s) AND reporter in (%s)",
                issueTypesToGet, allowedStatuses, escapeUsername(getUsername()));
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
        transitionIssue(transition, null);
    }

    public void transitionIssue(IssueTransition transition, IssueResolutionDefinition resolution) {
        IssueUpdate updateIssue = new IssueUpdate(transition);
        if (resolution != null) {
            updateIssue.fields.resolution = new IssueResolution(resolution);
        }
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

    public void updateIssueStoryPointsUsingAgileApi(Issue issue, String boardId) {
        IssueUpdate updateIssue = new IssueUpdate();
        updateIssue.fields.storyPoints = issue.fields.storyPoints;
        String url = agileUrl + "issue/" + issue.getKey() + "/estimation?boardId=" + boardId;
        StringValue storyPoints = new StringValue(String.valueOf(issue.fields.storyPoints));
        connection.put(url, storyPoints);
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
        connection.get(baseUrl + "rest/auth/1/session",null);
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
