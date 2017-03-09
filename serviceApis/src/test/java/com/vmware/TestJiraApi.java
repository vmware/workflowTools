package com.vmware;

import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTransition;
import com.vmware.jira.domain.IssueTransitions;
import com.vmware.jira.domain.IssueTypeDefinition;
import com.vmware.jira.domain.IssuesResponse;
import com.vmware.jira.domain.JiraUser;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.greenhopper.IssueSummary;
import com.vmware.jira.domain.greenhopper.RapidView;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestJiraApi extends BaseTests {

    private static Jira jira;

    private static String jiraIssueNumber;

    @BeforeClass
    public static void createIssue() {
        String jiraUsername = testProperties.getProperty("jira.username");
        String jiraUrl = testProperties.getProperty("jira.url");
        jira = new Jira(jiraUrl, "HW-1001", jiraUsername);
        jira.setupAuthenticatedConnection();
        Issue issueToCreate = new Issue(IssueTypeDefinition.Story, "HW", "Build and Infrastructure",
                "Test Issue", "Test Description", "Test criteria");
        issueToCreate.fields.assignee = new JiraUser(jiraUsername);
        Issue createdIssue = jira.createIssue(issueToCreate);
        assertNotNull(createdIssue);
        jiraIssueNumber = createdIssue.getKey();
    }

    @AfterClass
    public static void deleteIssue() {
        jira.deleteIssue(jiraIssueNumber);
    }

    @Before
    public void setIssueInProgress() {
        IssueTransitions issueTransitions = jira.getAllowedTransitions(jiraIssueNumber);
        if (issueTransitions.canTransitionTo(IssueStatusDefinition.InProgress)) {
            jira.transitionIssue(issueTransitions.getTransitionForStatus(IssueStatusDefinition.InProgress));
        }
    }

    @Test
    public void canGetRecentBoardItems() {
        List<MenuItem> boardItems = jira.getRecentBoardItems();
        assertTrue("Expected board items to be returned", boardItems.size() > 0);
    }

    @Test
    public void canGetBacklogStories() {
        List<MenuItem> boardItems = jira.getRecentBoardItems();
        assertTrue("Expected board items to be returned", boardItems.size() > 0);

        RapidView rapidView = jira.getRapidView(boardItems.get(0).getBoardId());
        List<IssueSummary> backlogStories = rapidView.getIssues(Arrays.asList(IssueTypeDefinition.Story), true);
        assertTrue("Expected board to have backlog stories", backlogStories.size() > 0);
    }

    @Test
    public void canGetJiraIssue() {
        Issue issue = jira.getIssueByKey(jiraIssueNumber);
        assertEquals(IssueStatusDefinition.InProgress, issue.getStatus());
    }

    @Test
    public void canGetAssignedJiraIssues() {
        IssuesResponse issues = jira.getOpenTasksForUser();
        assertTrue("No assigned issues found", issues.total > 0);
    }

    @Test
    public void canGetClosedIssuesWithoutResolution() {
        IssuesResponse issues = jira.getIssuesForUser(IssueStatusDefinition.Closed, null);
        assertTrue("No closed issues without resolution found", issues.total > 0);
    }

    @Test
    public void canGetCreatedJiraIssues() {
        IssuesResponse issues = jira.getCreatedTasksForUser();
        assertTrue("Expected to find created issue for user " + jira.getUsername(), issues.total > 0);
    }

    @Test
    public void canGetAllowedTransitionsForJiraIssue() {
        IssueTransitions transitionWrapper = jira.getAllowedTransitions(jiraIssueNumber);
        assertTrue(transitionWrapper.canTransitionTo(IssueStatusDefinition.InReview));
        assertFalse(transitionWrapper.canTransitionTo(IssueStatusDefinition.InProgress));
    }

    @Test
    public void canMarkIssueAsInReview() {
        IssueTransitions transitionWrapper = jira.getAllowedTransitions(jiraIssueNumber);
        assertNotNull(transitionWrapper);
        IssueTransition inReviewTransition = transitionWrapper.getTransitionForStatus(IssueStatusDefinition.InReview);
        jira.transitionIssue(inReviewTransition);

        Issue updatedIssue = jira.getIssueByKey(jiraIssueNumber);
        assertEquals(IssueStatusDefinition.InReview, updatedIssue.getStatus());
    }

    @Test
    public void canUpdateStoryPoints() {
        Issue issue = new Issue(jiraIssueNumber);
        issue.fields.storyPoints = 5;
        jira.updateIssue(issue);

        Issue updatedIssue = jira.getIssueByKey(jiraIssueNumber);
        assertEquals(5, updatedIssue.fields.storyPoints.intValue());
    }

    @Test
    public void canUpdateEstimateForIssue() {
        jira.updateIssueEstimate(jiraIssueNumber, 1);
    }

    @Test
    public void canResolveIssue() {
        IssueTransitions issueTransitions = jira.getAllowedTransitions(jiraIssueNumber);
        if (issueTransitions.canTransitionTo(IssueStatusDefinition.Resolved)) {
            jira.transitionIssue(issueTransitions.getTransitionForStatus(IssueStatusDefinition.Resolved), IssueResolutionDefinition.Fixed);
        }
        issueTransitions = jira.getAllowedTransitions(jiraIssueNumber);
        jira.transitionIssue(issueTransitions.getTransitionForStatus(IssueStatusDefinition.Reopened));
    }

}
