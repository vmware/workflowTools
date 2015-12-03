package com.vmware.action.commitInfo;

import com.vmware.IssueInfo;
import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractCommitReadAction;
import com.vmware.bugzilla.domain.Bug;
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
            issues = getJiraIssues(bugNumbers, draft.openIssues);
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

    private void loadJiraIssuesList(ReviewRequestDraft draft) throws IOException, URISyntaxException, IllegalAccessException {
        if (!draft.isPreloadingJiraIssues && draft.openIssues == null) {
            jira.setupAuthenticatedConnection();
            draft.openIssues = jira.getOpenTasksForUser(config.username).issues;
        } else if (draft.openIssues == null) {
            log.info("Jira issue list not loaded yet, waiting 3 seconds");
            ThreadUtils.sleep(3000);
            if (draft.openIssues == null) {
                log.info("Failed to load jira issues");
                draft.openIssues = new Issue[0];
            }
        }
    }

    private void printIssuesList(IssueInfo[] issues) {
        if (issues.length > 0) {
            log.info("Assigned Jira Tasks, list number can be entered as shorthand for bug number\ne.g. 1 for " + issues[0].getKey());
            log.info("Alternatively, enter the full bug number (assuming bug starts with {} if only numeric part entered)", config.bugPrefix);
            log.info("");
            for (int i = 0; i < issues.length; i ++) {
                IssueInfo issue = issues[i];
                log.info("[{}] {}: {}", (i + 1), issue.getKey(), issue.getSummary());
            }
        } else {
            log.info("Assuming bug starts with {} if only numeric part entered", config.bugPrefix);
        }
    }

    private List<IssueInfo> getJiraIssues(String bugNumberText, IssueInfo[] preloadedIssues) throws IOException, URISyntaxException {
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
            issues.add(getIssues(preloadedIssues, bugNumber.trim()));
        }
        if (issues.contains(Issue.noBugNumber)) {
            issues.retainAll(Arrays.asList(Issue.noBugNumber));
        }

        return issues;
    }

    private IssueInfo getIssues(IssueInfo[] preloadedIssues, String bugNumber) throws IOException, URISyntaxException {
        if (bugNumber.equals(config.noBugNumberLabel)) {
            return Issue.noBugNumber;
        }
        if (config.isBugzillaBug(bugNumber)) {
            return Bug.aBug(config.bugzillaPrefix, bugNumber);
        } else if (StringUtils.isInteger(bugNumber)) {
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
