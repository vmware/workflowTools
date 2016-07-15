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
        syncPerforceFiles(gitDiffChanges, lastSubmittedChangelistInfo[1]);
        String fromRef = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo[0] : config.trackingBranch;
        if (config.syncChangelistToLatestInBranch) {
            log.info("Syncing files to be modified to changelist {} and applying diff", lastSubmittedChangelistInfo[1]);
        } else {
            log.info("Syncing files to be modified to latest");
        }
        String diffData = git.diffTree(fromRef, "head", true);

        String checkOutput = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, true);

        if (StringUtils.isNotBlank(checkOutput)) {
            log.debug("Failed diff\n{}", diffData);
            throw new IllegalArgumentException("Check of git diff failed!\n" + checkOutput);
        }

        openExistingFilesForEditIfNeeded(gitDiffChanges);

        String output = git.applyDiffToPerforce(perforce.getWorkingDirectory() + File.separator, diffData, false);
        if (StringUtils.isNotBlank(checkOutput)) {
            perforce.revertChangesInPendingChangelist(draft.perforceChangelistId);
            perforce.clean();
            throw new IllegalArgumentException("Unexpected output when applying git diff!\n" + output);
        }
        log.info("Applied git diff to changelist {}", draft.perforceChangelistId);

        makePerforceAwareOfDiffChanges(gitDiffChanges);
        log.info("Ran p4 move,add,edit,delete for affected files");
    }

    private void syncPerforceFiles(List<FileChange> gitDiffChanges, String lastSubmittedChangelistId) {
        List<String> filesToSync = new ArrayList<>();
        for (FileChange diffChange : gitDiffChanges) {
            filesToSync.add(perforce.fullPath(diffChange.getFirstFileAffected()));
        }

        String syncVersion = ""; // empty means latest
        if (config.syncChangelistToLatestInBranch) {
            syncVersion = "@" + lastSubmittedChangelistId;
        }

        if (!filesToSync.isEmpty()) {
            log.info("Syncing existing perforce files {}", filesToSync.toString());
            perforce.sync(StringUtils.appendWithDelimiter("", filesToSync, syncVersion +" ") + syncVersion);
        }
    }

    private void openExistingFilesForEditIfNeeded(List<FileChange> gitDiffChanges) {
        List<String> filesToOpenForEdit = new ArrayList<>();

        for (FileChange diffChange : gitDiffChanges) {
            if (FileChangeType.isEditChangeType(diffChange.getChangeType())) {
                String fullPath = perforce.fullPath(diffChange.getFirstFileAffected());
                filesToOpenForEdit.add(fullPath);
            }
        }
        if (!filesToOpenForEdit.isEmpty()) {
            perforce.openForEdit(draft.perforceChangelistId, StringUtils.appendWithDelimiter("", filesToOpenForEdit, " "));
        }
    }

    private void makePerforceAwareOfDiffChanges(List<FileChange> gitDiffChanges) {
        for (FileChange diffChange : gitDiffChanges) {
            FileChangeType changeType = diffChange.getChangeType();
            String fullPathForFirstFileAffected = perforce.fullPath(diffChange.getFirstFileAffected());
            String fullPathForLastFileAffected = perforce.fullPath(diffChange.getLastFileAffected());
            if (changeType == FileChangeType.renamed || changeType == FileChangeType.renamedAndModified) {
                log.info("Renaming file {} to {}", diffChange.getFirstFileAffected(), diffChange.getLastFileAffected());
                perforce.move(draft.perforceChangelistId, fullPathForFirstFileAffected, fullPathForLastFileAffected, "-k");
            } else if (changeType == FileChangeType.added || changeType == FileChangeType.addedAndModified) {
                log.info("Adding file {} to perforce", diffChange.getLastFileAffected());
                perforce.add(draft.perforceChangelistId, fullPathForLastFileAffected);
            } else if (changeType == FileChangeType.deleted) {
                log.info("Deleting {}", diffChange.getLastFileAffected());
                perforce.markForDelete(draft.perforceChangelistId, fullPathForLastFileAffected);
            }
        }
    }
}
