package com.vmware.action.conditional;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.DiffUtils;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;
import com.vmware.util.scm.GitChangelistRef;
import com.vmware.util.scm.diff.PendingChangelistToGitDiffCreator;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.util.scm.FileChange.containsChangesOfType;
import static com.vmware.util.scm.FileChangeType.added;
import static com.vmware.util.scm.FileChangeType.addedAndModified;
import static com.vmware.util.scm.FileChangeType.deleted;
import static com.vmware.util.StringUtils.stripLinesStartingWith;

@ActionDescription("Creates a diff for the changelist and compares it to a diff of the current git branch.")
public class ExitIfChangelistDoesNotMatchGitBranch extends BaseLinkedPerforceCommitUsingGitAction {

    public ExitIfChangelistDoesNotMatchGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Checking if changelist {} matches current git branch", draft.perforceChangelistId);
        GitChangelistRef lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = buildwebConfig.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo.getCommitRef() : gitRepoConfig.trackingBranchPath();
        if (buildwebConfig.syncChangelistToLatestInBranch) {
            log.info("Creating git diff against last submitted changelist {}", lastSubmittedChangelistInfo.getChangelistId());
        } else {
            log.info("Creating git diff against tracking branch {}", gitRepoConfig.trackingBranchPath());
        }

        List<FileChange> fileChanges = perforce.getFileChangesForPendingChangelist(draft.perforceChangelistId);
        String rawGitDiff = git.diffTree(fromRef, "head", true, LogLevel.TRACE);
        String gitDiff;
        if (containsChangesOfType(fileChanges, FileChangeType.renamed)) {
            gitDiff = stripLinesStartingWith(rawGitDiff, "similarity index");
        } else {
            gitDiff = rawGitDiff;
        }
        log.info("Creating perforce diff for changelist {} in git format", draft.perforceChangelistId);

        PendingChangelistToGitDiffCreator diffCreator = new PendingChangelistToGitDiffCreator(perforce);
        String perforceDiff = diffCreator.create(draft.perforceChangelistId, fileChanges, LogLevel.TRACE);
        if (StringUtils.equals(gitDiff, perforceDiff)) {
            log.info("Perforce diff matches git diff exactly");
            return;
        } else {
            log.info("Perforce diff didn't match git diff exactly, comparing diffs in terms of content");
        }

        String reasonForMotMatching = DiffUtils.compareDiffContent(gitDiff, perforceDiff, "git", "perforce");
        if (reasonForMotMatching != null) {
            log.error("Perforce diff didn't match git diff\n{}\n", reasonForMotMatching);
            cancelWithErrorMessage("You might need to pull and rebase your git branch against the latest code.");
        } else if (containsChangesOfType(fileChanges, added, addedAndModified, deleted)) {
            log.info("Perforce diff matches git diff in terms of content, adding or deleting file causes diff ordering to be different for perforce.");
        } else {
            log.info("Perforce diff matches git diff in terms of content.");
        }
    }
}
