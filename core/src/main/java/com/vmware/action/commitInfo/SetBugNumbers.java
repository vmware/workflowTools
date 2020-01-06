package com.vmware.action.commitInfo;

import com.vmware.IssueInfo;
import com.vmware.action.base.BaseCommitReadAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ActionDescription("Sets the bug number. A list of assigned jira bugs and tasks is shown for easy selection. Bug number can also be manually entered.")
public class SetBugNumbers extends BaseCommitReadAction {

    private Jira jira = null;

    private Bugzilla bugzilla = null;

    private boolean jiraIssuesLoaded, bugzillaBugsLoaded, userHasBugzillaQuery;;

    private List<IssueInfo> loadedIssues = new ArrayList<>();

    public SetBugNumbers(WorkflowConfig config){
        super(config, "bugNumbers");
    }

    @Override
    public void asyncSetup() {
        loadedIssues.clear();
        if (!jiraConfig.disableJira) {
            loadJiraIssues();
        }
        if (!bugzillaConfig.disableBugzilla) {
            loadBugzillaBugs();
        }
    }

    @Override
    public void process() {
        loadIssuesList(draft);
        printIssuesList();
        List<IssueInfo> issues = null;
        if (StringUtils.isNotEmpty(draft.bugNumbers) && !draft.bugNumbers.equals(commitConfig.noBugNumberLabel)) {
            log.info("");
            log.info("Existing bug numbers: " + draft.bugNumbers);
        }

        boolean waitingForBugNumbers = true;
        while (waitingForBugNumbers) {
            String bugNumbers = InputUtils.readData("Bug Numbers: (leave blank if none)", true, 30);
            issues = getBugsAndIssues(bugNumbers);
            waitingForBugNumbers = false;
            if (listHasNoBugNumbers(issues)) {
                draft.bugNumbers = commitConfig.noBugNumberLabel;
            } else if (!allIssuesWereFound(issues)) {
                String reenterBugNumber = InputUtils.readValue("One or more issues not found, reenter bug numbers? [y/n]");
                waitingForBugNumbers = reenterBugNumber.equalsIgnoreCase("y");
            }
        }
        draft.setIssues(issues, commitConfig.noBugNumberLabel);
        log.info("Bug numbers for commit: {}", draft.bugNumbers);
    }

    private void loadJiraIssues() {
        this.jira = serviceLocator.getJira();
        if (!jira.isBaseUriTrusted() || !jira.isConnectionAuthenticated()) {
            return;
        }

        loadedIssues.addAll(Arrays.asList(jira.getOpenTasksForUser().issues));
        jiraIssuesLoaded = true;
    }

    private void loadBugzillaBugs() {
        bugzilla = serviceLocator.getBugzilla();
        if (!bugzilla.isBaseUriTrusted() || !bugzilla.isConnectionAuthenticated()) {
            return;
        }
        if (bugzilla.containsSavedQuery(bugzillaConfig.bugzillaQuery)) {
            userHasBugzillaQuery = true;
            loadedIssues.addAll(bugzilla.getBugsForQuery(bugzillaConfig.bugzillaQuery));
        }
        bugzillaBugsLoaded = true;
    }

    private void loadIssuesList(ReviewRequestDraft draft) {
        if (jira == null && bugzilla == null) {
            log.info("Both Jira and Bugzilla are disabled, no issues can be loaded");
            return;
        }

        if (jira != null && !jiraIssuesLoaded) {
            jira.setupAuthenticatedConnection();
            loadJiraIssues();
        }

        if (bugzilla != null && !bugzillaBugsLoaded) {
            bugzilla.setupAuthenticatedConnection();
            loadBugzillaBugs();
        }
    }

    private void printIssuesList() {
        if (bugzilla != null && !userHasBugzillaQuery) {
            log.info("\n**  Can't load your Bugzilla bugs as saved query {} not found in your bugzilla query list  **" +
                    "\nPlease create if you want to easily select a bugzilla bug", bugzillaConfig.bugzillaQuery);
        }
        if (loadedIssues.size() > 0) {
            IssueInfo firstIssue = loadedIssues.iterator().next();
            log.info("Assigned bugs / issues, list number can be entered as shorthand for id\ne.g. 1 for " + firstIssue.getKey());
            log.info("Alternatively, enter the full id");
            log.info("");
            int counter = 0;
            for (IssueInfo issue : loadedIssues) {
                log.info("[{}] {}: {}", ++counter, issue.getKey(), issue.getSummary());
            }
        }
    }

    private List<IssueInfo> getBugsAndIssues(String bugNumberText) {
        if (bugNumberText == null || bugNumberText.isEmpty()) {
            bugNumberText = commitConfig.noBugNumberLabel;
        }

        List<IssueInfo> issues = new ArrayList<>();
        String[] bugNumbers = bugNumberText.split(",");
        for (String bugNumber: bugNumbers) {
            String trimmedBugNumber = bugNumber.trim();
            if (trimmedBugNumber.isEmpty()) {
                continue;
            }
            issues.add(getIssue(bugNumber.trim()));
        }
        if (issues.contains(Issue.noBugNumber)) {
            issues.retainAll(Collections.singletonList(Issue.noBugNumber));
        }

        return issues;
    }

    private IssueInfo getIssue(String bugNumber) {
        if (bugNumber.equals(commitConfig.noBugNumberLabel)) {
            return Issue.noBugNumber;
        } else if (StringUtils.isInteger(bugNumber)) {
            int number = Integer.parseInt(bugNumber);
            if (number <= loadedIssues.size()) {
                return loadedIssues.get(number - 1);
            }
        }
        // prepend with prefix if just a number was entered
        String fullJiraKey = getFullJiraKey(bugNumber);
        // test that bug number is a valid jira issue or bugzilla bug
        IssueInfo issueInfo = Issue.aNotFoundIssue(bugNumber);
        Integer bugzillaBugNumber = bugzillaConfig.parseBugzillaBugNumber(bugNumber);
        if (config.bugNumberSearchOrder.indexOf("Bugzilla") == 0) {
            if (bugzilla != null && bugzillaBugNumber != null) {
                issueInfo = bugzilla.getBugByIdWithoutException(bugzillaBugNumber);
            }
            if (issueInfo.isNotFound() && jira != null) {
                issueInfo = jira.getIssueWithoutException(fullJiraKey);
            }
        } else {
            if (jira != null) {
                issueInfo = jira.getIssueWithoutException(fullJiraKey);
            }
            if (issueInfo.isNotFound() && bugzilla != null && bugzillaBugNumber != null) {
                issueInfo = bugzilla.getBugByIdWithoutException(bugzillaBugNumber);
            }
        }
        return issueInfo;
    }

    private String getFullJiraKey(String bugNumber) {
        if (!StringUtils.isInteger(bugNumber)) {
            return bugNumber;
        }
        return jiraConfig.defaultJiraProject + "-" + bugNumber;
    }

    private boolean allIssuesWereFound(List<IssueInfo> issues) {
        boolean allFound = true;
        Padder issuePadder = new Padder("Selected Issues");
        issuePadder.infoTitle();
        for (IssueInfo issue : issues) {
            if (issue.isNotFound()) {
                allFound = false;
                log.warn("Issue with key {} was not found", issue.getKey());
            } else {
                log.info("{}: {}", issue.getKey(), issue.getSummary());
            }
        }
        issuePadder.infoTitle();
        return allFound;
    }

    private boolean listHasNoBugNumbers(List<IssueInfo> jiraIssues) {
        for (IssueInfo jiraIssue: jiraIssues) {
            if (jiraIssue == Issue.noBugNumber) {
                return true;
            }
        }
        return false;
    }
}
