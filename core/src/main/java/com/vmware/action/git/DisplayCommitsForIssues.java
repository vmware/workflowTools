
package com.vmware.action.git;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.logging.Padder;

@ActionDescription("Displays a listing of commits for the loaded issues")
public class DisplayCommitsForIssues extends BaseIssuesProcessingAction {
    public DisplayCommitsForIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<String> issueKeys = projectIssues.getIssuesFromJira().stream().map(Issue::getKey).collect(Collectors.toList());
        if (gitRepoConfig.sinceDate != null) {
            log.info("Checking commits since {} for following {} jira issues\n{}", gitRepoConfig.sinceDate, issueKeys.size(), issueKeys);
        } else {
            log.info("Checking last {} commits for following {} jira issues\n{}", gitRepoConfig.maxCommitsToCheck, issueKeys.size(), issueKeys);
        }

        Padder commitPadder = new Padder("Matching Commits");

        List<ReviewRequestDraft> matchingCommits = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        List<String> commitTexts = gitRepoConfig.sinceDate != null ? git.commitsSince(gitRepoConfig.sinceDate) : git.commitTexts(gitRepoConfig.maxCommitsToCheck);
        if (gitRepoConfig.sinceDate != null || commitTexts.size() != gitRepoConfig.maxCommitsToCheck) {
            log.info("Checking {} commits", commitTexts.size());
        }

        commitPadder.infoTitle();
        for (String commitText : commitTexts) {
            ReviewRequestDraft parsedCommit = new ReviewRequestDraft(commitText, commitConfig);
            if (parsedCommit.bugNumbers != null && issueKeys.stream().anyMatch(key -> parsedCommit.bugNumbers.contains(key))) {
                log.info("{} {} {} {}", parsedCommit.commitId, formatter.format(parsedCommit.commitDate), parsedCommit.authorName, parsedCommit.summary);
                matchingCommits.add(parsedCommit);
            }
        }
        commitPadder.infoTitle();
        log.info("Found {} matching commits", matchingCommits.size());
    }
}
