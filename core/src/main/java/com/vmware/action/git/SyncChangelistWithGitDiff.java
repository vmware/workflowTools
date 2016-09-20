package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.scm.FileChange;
import com.vmware.scm.FileChangeType;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ActionDescription("Syncs the contents of the linked changelist with a git diff against the last submitted changelist in the current branch.")
public class SyncChangelistWithGitDiff extends BaseLinkedPerforceCommitAction {
    public SyncChangelistWithGitDiff(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<FileChange> gitDiffChanges = git.getChangesInDiff(config.parentBranch, "head");
        log.debug("Git diff change count {}", gitDiffChanges.size());

        String[] lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String fromRef = determineRefToDiffAgainst(lastSubmittedChangelistInfo);
        String versionToSyncTo = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo[1] : "";
        perforce.syncPerforceFiles(gitDiffChanges, versionToSyncTo);
        String diffData = git.diffTree(fromRef, "head", true);

        String checkOutput = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, true);

        if (StringUtils.isNotBlank(checkOutput)) {
            log.debug("Failed diff\n{}", diffData);
            throw new IllegalArgumentException("Check of git diff failed!\n" + checkOutput);
        }

        perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, gitDiffChanges);

        String output = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, false);
        if (StringUtils.isNotBlank(checkOutput)) {
            perforce.revertChangesInPendingChangelist(draft.perforceChangelistId);
            perforce.clean();
            throw new IllegalArgumentException("Unexpected output when applying git diff!\n" + output);
        }
        log.info("Applied git diff to changelist {}", draft.perforceChangelistId);

        perforce.renameAddOrDeleteFiles(draft.perforceChangelistId, gitDiffChanges);
        log.info("Ran p4 move,add,edit,delete for affected files");
    }

    private String determineRefToDiffAgainst(String[] lastSubmittedChangelistInfo) {
        String fromRef;
        if (config.syncChangelistToLatestInBranch) {
            log.info("Syncing files to be modified to changelist {} and applying diff", lastSubmittedChangelistInfo[1]);
            fromRef = lastSubmittedChangelistInfo[0];
        } else {
            log.info("Syncing files to be modified to latest");
            fromRef = config.trackingBranch;
        }
        return fromRef;
    }
}
