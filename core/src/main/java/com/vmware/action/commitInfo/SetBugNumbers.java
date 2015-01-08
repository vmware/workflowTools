package com.vmware.action.commitInfo;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractCommitReadAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;
import com.vmware.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ActionDescription("Sets the bug number. A list of assigned jira bugs and tasks is shown for easy selection. Bug number can also be manually entered.")
public class SetBugNumbers extends AbstractCommitReadAction {

    private static Logger log = LoggerFactory.getLogger(SetBugNumbers.class.getName());

    private Jira jira;

    public SetBugNumbers(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException, NoSuchFieldException {
        super(config, "bugNumbers");
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jira = ServiceLocator.getJira(config.jiraUrl, false);
    }


    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        loadJiraIssuesList(draft);
        printIssuesList(draft.openJiraIssues);
        boolean waitingForBugNumbers = true;
        List<Issue> jiraIssues = null;
        if (StringUtils.isNotBlank(draft.bugNumbers)) {
            log.info("");
            log.info("");
            log.info("Existing bug numbers: " + draft.bugNumbers);
        }

        while (waitingForBugNumbers) {
            String bugNumbers = InputUtils.readData("Bug Numbers: (leave blank if none)", true, 30);
            jiraIssues = getJiraIssues(bugNumbers, draft.openJiraIssues);
            waitingForBugNumbers = false;
            if (listHasNoBugNumbers(jiraIssues)) {
                draft.bugNumbers = config.noBugNumberLabel;
            } else if (!allIssuesWereFound(jiraIssues)) {
                String reenterBugNumber = InputUtils.readValue("One or more issues not found, reenter bug numbers? [y/n]");
                waitingForBugNumbers = reenterBugNumber.equalsIgnoreCase("y");
            }
        }
        draft.setJiraIssues(jiraIssues, config.noBugNumberLabel);
        log.info("Bug numbers for commit: {}", draft.bugNumbers);
    }

    private void loadJiraIssuesList(ReviewRequestDraft draft) throws IOException, URISyntaxException, IllegalAccessException {
        if (!draft.isPreloadingJiraIssues && draft.openJiraIssues == null) {
            jira.setupAuthenticatedConnection();
            draft.openJiraIssues = jira.getOpenTasksForUser(config.username).issues;
        } else if (draft.openJiraIssues == null) {
            log.info("Jira issue list not loaded yet, waiting 3 seconds");
            ThreadUtils.sleep(3000);
            if (draft.openJiraIssues == null) {
                log.info("Failed to load jira issues");
                draft.openJiraIssues = new Issue[0];
            }
        }
    }

    private void printIssuesList(Issue[] issues) {
        if (issues.length > 0) {
            log.info("Assigned Jira Tasks, list number can be entered as shorthand for bug number\ne.g. 1 for " + issues[0].key);
            log.info("Alternatively, enter the full bug number (assuming bug starts with {} if only numeric part entered)", config.bugPrefix);
            log.info("");
            for (int i = 0; i < issues.length; i ++) {
                Issue issue = issues[i];
                log.info("[{}] {}: {}", (i + 1), issue.key, issue.fields.summary);
            }
        } else {
            log.info("Assuming bug starts with {} if only numeric part entered", config.bugPrefix);
        }
    }

    private List<Issue> getJiraIssues(String bugNumberText, Issue[] issues) throws IOException, URISyntaxException {
        if (bugNumberText == null || bugNumberText.isEmpty()) {
            bugNumberText = config.noBugNumberLabel;
        }

        List<Issue> jiraIssues = new ArrayList<Issue>();
        String[] bugNumbers = bugNumberText.split(",");
        for (String bugNumber: bugNumbers) {
            String trimmedBugNumber = bugNumber.trim();
            if (trimmedBugNumber.isEmpty()) {
                continue;
            }
            jiraIssues.add(getJiraIssue(issues, bugNumber.trim()));
        }
        if (jiraIssues.contains(Issue.noBugNumber)) {
            jiraIssues.retainAll(Arrays.asList(Issue.noBugNumber));
        }

        return jiraIssues;
    }

    private Issue getJiraIssue(Issue[] preloadedIssues, String bugNumber) throws IOException, URISyntaxException {
        if (bugNumber.equals(config.noBugNumberLabel)) {
            return Issue.noBugNumber;
        }
        if (StringUtils.isInteger(bugNumber)) {
            int number = Integer.parseInt(bugNumber);
            if (number <= preloadedIssues.length) {
                return preloadedIssues[number-1];
            }
            bugNumber = config.bugPrefix + "-" + bugNumber;
        }
        // test that bug number is a valid jira issue
        try {
            return jira.getIssueByKey(bugNumber);
        } catch (NotFoundException e) {
            log.debug(e.getMessage(), e);
            return Issue.aNotFoundIssue(bugNumber);
        }
    }

    private boolean allIssuesWereFound(List<Issue> jiraIssues) {
        boolean allFound = true;
        Padder issuePadder = new Padder("Selected Issues");
        issuePadder.infoTitle();
        for (Issue jiraIssue : jiraIssues) {
            if (jiraIssue.isNotFound) {
                allFound = false;
                log.warn("Issue with key {} was not found", jiraIssue.key);
            } else {
                log.info("{}: {}", jiraIssue.key, jiraIssue.fields.summary);
            }
        }
        issuePadder.infoTitle();
        return allFound;
    }

    private boolean listHasNoBugNumbers(List<Issue> jiraIssues) {
        for (Issue jiraIssue: jiraIssues) {
            if (jiraIssue == Issue.noBugNumber) {
                return true;
            }
        }
        return false;
    }
}
