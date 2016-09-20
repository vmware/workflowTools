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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ActionDescription("Syncs changelist by copying changed files from git to the perforce client directory.")
public class SyncChangelist extends BaseLinkedPerforceCommitAction {
    public SyncChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Date startingDate = new Date();
        log.info("Syncing changes to changelist {}", draft.perforceChangelistId);
        List<FileChange> gitDiffChanges = git.getChangesInDiff(config.parentBranch, "head");
        log.debug("Git diff change count {}", gitDiffChanges.size());

        String[] lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String versionToSyncTo = config.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo[1] : "";
        perforce.syncPerforceFiles(gitDiffChanges, versionToSyncTo);

        Map<String, List<FileChange>> allPerforceChanges = perforce.getAllFileChangesInClient();
        revertChangesNotInGitDiff(gitDiffChanges, allPerforceChanges);
        List<FileChange> missingChanges = makePerforceAwareOfMissingChanges(gitDiffChanges, allPerforceChanges);
        copyChangedFilesToClient(gitDiffChanges);
        perforce.renameAddOrDeleteFiles(draft.perforceChangelistId, missingChanges);
        long elapsedTime = new Date().getTime() - startingDate.getTime();
        log.info("Synced changes to changelist {} in {} seconds\n", draft.perforceChangelistId,
                TimeUnit.MILLISECONDS.toSeconds(elapsedTime));

    }

    private void copyChangedFilesToClient(List<FileChange> fileChanges) {
        for (FileChange fileChange : fileChanges) {
            FileChangeType changeType = fileChange.getChangeType();
            if (!FileChangeType.isAddChangeType(changeType) && !FileChangeType.isEditChangeType(changeType)) {
                continue;
            }

            File fileInGit = new File(git.fullPath(fileChange.getLastFileAffected()));
            File fileInPerforce = new File(perforce.fullPath(fileChange.getLastFileAffected()));

            git.catFile("head", fileChange.getLastFileAffected(), fileInPerforce.getPath());
            if (fileInGit.canExecute() != fileInPerforce.canExecute()) {
                fileInPerforce.setExecutable(fileInGit.canExecute());
                log.debug("Changing file {} executable to {}", fileChange.getLastFileAffected(), fileInGit.canExecute());
            }
        }
    }

    private List<FileChange> makePerforceAwareOfMissingChanges(List<FileChange> gitDiffChanges, Map<String, List<FileChange>> allPerforceChanges) {
        List<FileChange> changesToAddToPerforce = new ArrayList<>(gitDiffChanges);
        for (String changelistId : allPerforceChanges.keySet()) {
            for (FileChange fileChange : allPerforceChanges.get(changelistId)) {
                if (!gitDiffChanges.contains(fileChange)) {
                    continue;
                }
                FileChange gitFileChange = gitDiffChanges.get(gitDiffChanges.indexOf(fileChange));
                gitFileChange.setPerforceChangelistId(changelistId);
            }
        }

        perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, changesToAddToPerforce);

        return changesToAddToPerforce;
    }

    private void revertChangesNotInGitDiff(List<FileChange> gitDiffChanges, Map<String, List<FileChange>> allPerforceChanges) {
        if (!allPerforceChanges.containsKey(draft.perforceChangelistId)) {
            return;
        }

        List<FileChange> perforceChangesToRemove = new ArrayList<>(allPerforceChanges.get(draft.perforceChangelistId));
        perforceChangesToRemove.removeAll(gitDiffChanges);
        List<String> pathsToRevert = filePathsForChanges(perforceChangesToRemove, true);
        if (!pathsToRevert.isEmpty()) {
            log.info("Reverting following perforce changes not in git diff\n{}", pathsToRevert.toString());
            perforce.revertFiles(draft.perforceChangelistId, pathsToRevert);
        }
    }

    private List<String> filePathsForChanges(List<FileChange> changes, boolean lastPathAffected) {
        List<String> filePaths = new ArrayList<>();
        for (FileChange change : changes) {
            String pathToUse = lastPathAffected ? change.getLastFileAffected() : change.getFirstFileAffected();
            filePaths.add(perforce.fullPath(pathToUse));
        }
        return filePaths;
    }

    private List<FileChange> allChangesInMap(Map<String, List<FileChange>> changeMap) {
        List<FileChange> allChanges = new ArrayList<>();
        for (String key : changeMap.keySet()) {
            allChanges.addAll(changeMap.get(key));
        }
        return allChanges;
    }



}
