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
        log.info("Checking last {} commits for following {} jira issues\n{}", gitRepoConfig.maxCommitsToCheck, issueKeys.size(), issueKeys);

        Padder commitPadder = new Padder("Matching Commits");

        List<ReviewRequestDraft> matchingCommits = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        commitPadder.infoTitle();
        List<String> commitTexts = git.commitTexts(gitRepoConfig.maxCommitsToCheck);
        for (int i = 0; i < commitTexts.size(); i ++) {
            ReviewRequestDraft parsedCommit = new ReviewRequestDraft(commitTexts.get(i), commitConfig);
            if (parsedCommit.bugNumbers != null && issueKeys.stream().anyMatch(key -> parsedCommit.bugNumbers.contains(key))) {
                log.info("{} {} {} {}", parsedCommit.commitId, formatter.format(parsedCommit.commitDate), parsedCommit.authorName, parsedCommit.summary);
                matchingCommits.add(parsedCommit);
            }
        }
        commitPadder.infoTitle();
        log.info("Found {} matching commits", matchingCommits.size());
    }
}
