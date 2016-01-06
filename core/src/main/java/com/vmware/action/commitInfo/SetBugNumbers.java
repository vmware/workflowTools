package com.vmware.action.commitInfo;

import com.vmware.IssueInfo;
import com.vmware.action.base.AbstractCommitReadAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;
import com.vmware.utils.ThreadUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ActionDescription("Sets the bug number. A list of assigned jira bugs and tasks is shown for easy selection. Bug number can also be manually entered.")
public class SetBugNumbers extends AbstractCommitReadAction {

    private Jira jira = null;

    private Bugzilla bugzilla = null;

    public SetBugNumbers(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException, NoSuchFieldException {
        super(config, "bugNumbers");
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        if (!config.disableJira) {
            this.jira = serviceLocator.getUnauthenticatedJira();
        }
        if (!config.disableBugzilla) {
            this.bugzilla = serviceLocator.getUnauthenticatedBugzilla();
        }
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        loadIssuesList(draft);
        printIssuesList(draft.openIssues);
        boolean waitingForBugNumbers = true;
        List<IssueInfo> issues = null;
        if (StringUtils.isNotBlank(draft.bugNumbers)) {
            log.info("");
            log.info("");
            log.info("Existing bug numbers: " + draft.bugNumbers);
        }

        while (waitingForBugNumbers) {
            String bugNumbers = InputUtils.readData("Bug Numbers: (leave blank if none)", true, 30);
            issues = getBugsAndIssues(bugNumbers, draft.openIssues);
            waitingForBugNumbers = false;
            if (listHasNoBugNumbers(issues)) {
                draft.bugNumbers = config.noBugNumberLabel;
            } else if (!allIssuesWereFound(issues)) {
                String reenterBugNumber = InputUtils.readValue("One or more issues not found, reenter bug numbers? [y/n]");
                waitingForBugNumbers = reenterBugNumber.equalsIgnoreCase("y");
            }
        }
        draft.setIssues(issues, config.noBugNumberLabel);
        log.info("Bug numbers for commit: {}", draft.bugNumbers);
    }

    private void loadIssuesList(ReviewRequestDraft draft) throws IOException, URISyntaxException, IllegalAccessException {
        if (jira == null && bugzilla == null) {
            log.info("Both Jira and Bugzilla are disabled, no issues can be loaded");
            draft.openIssues = new ArrayList<>();
            return;
        }

        if (!draft.isPreloadingJiraIssues && !draft.isPreloadingBugzillaBugs && draft.openIssues == null) {
            if (jira != null) {
                jira.setupAuthenticatedConnection();
                draft.addIssues( jira.getOpenTasksForUser(config.username).issues);
            }
            if (bugzilla != null) {
                bugzilla.setupAuthenticatedConnection();
                if (bugzilla.containsSavedQuery(config.bugzillaQuery)) {
                    draft.userHasBugzillaQuery = true;
                    draft.addBugs(bugzilla.getBugsForQuery(config.bugzillaQuery));
                }
            }
        } else if (draft.openIssues == null) {
            log.info("Jira / Bugzilla lists not loaded yet, waiting 3 seconds");
            ThreadUtils.sleep(3, TimeUnit.SECONDS);
            if (draft.openIssues == null) {
                log.info("Failed to load Jira / Bugzilla items, Jira in progress {}, Bugzilla in progress",
                        draft.isPreloadingJiraIssues, draft.isPreloadingBugzillaBugs);
                draft.openIssues = new ArrayList<>();
            }
        }
    }

    private void printIssuesList(Collection<IssueInfo> issues) {
        if (bugzilla != null && !draft.userHasBugzillaQuery) {
            log.info("\n**  Can't load your Bugzilla bugs as saved query {} not found in your bugzilla query list  **" +
                    "\nPlease create if you want to easily select a bugzilla bug", config.bugzillaQuery);
        }
        if (issues.size() > 0) {
            IssueInfo firstIssue = issues.iterator().next();
            log.info("Assigned bugs / issues, list number can be entered as shorthand for id\ne.g. 1 for " + firstIssue.getKey());
            log.info("Alternatively, enter the full id");
            log.info("");
            int counter = 0;
            for (IssueInfo issue : issues) {
                log.info("[{}] {}: {}", ++counter, issue.getKey(), issue.getSummary());
            }
        }
    }

    private List<IssueInfo> getBugsAndIssues(String bugNumberText, List<IssueInfo> preloadedIssues) throws IOException, URISyntaxException {
        if (bugNumberText == null || bugNumberText.isEmpty()) {
            bugNumberText = config.noBugNumberLabel;
        }

        List<IssueInfo> issues = new ArrayList<>();
        String[] bugNumbers = bugNumberText.split(",");
        for (String bugNumber: bugNumbers) {
            String trimmedBugNumber = bugNumber.trim();
            if (trimmedBugNumber.isEmpty()) {
                continue;
            }
            issues.add(getIssue(preloadedIssues, bugNumber.trim()));
        }
        if (issues.contains(Issue.noBugNumber)) {
            issues.retainAll(Arrays.asList(Issue.noBugNumber));
        }

        return issues;
    }

    private IssueInfo getIssue(List<IssueInfo> preloadedIssues, String bugNumber) throws IOException, URISyntaxException {
        if (bugNumber.equals(config.noBugNumberLabel)) {
            return Issue.noBugNumber;
        } else if (StringUtils.isInteger(bugNumber)) {
            int number = Integer.parseInt(bugNumber);
            if (number <= preloadedIssues.size()) {
                return preloadedIssues.get(number - 1);
            }
        }
        // prepend with prefix if just a number was entered
        String fullJiraKey = getFullJiraKey(bugNumber);
        // test that bug number is a valid jira issue or bugzilla bug
        IssueInfo issueInfo = Issue.aNotFoundIssue(bugNumber);
        Integer bugzillaBugNumber = config.parseBugzillaBugNumber(bugNumber);
        if (config.getSearchOrderForService("Bugzilla") == 0) {
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
        return config.defaultJiraProject + "-" + bugNumber;
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
