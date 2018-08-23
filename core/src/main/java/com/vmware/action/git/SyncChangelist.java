package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.scm.FileChange;
import com.vmware.util.scm.FileChangeType;
import com.vmware.util.scm.GitChangelistRef;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ActionDescription("Syncs changelist by copying changed files from git to the perforce client directory.")
public class SyncChangelist extends BaseLinkedPerforceCommitUsingGitAction {
    public SyncChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Date startingDate = new Date();
        log.info("Syncing changes to changelist {}", draft.perforceChangelistId);
        List<FileChange> gitDiffChanges = git.getChangesInDiff(gitRepoConfig.trackingBranchPath(), "head");
        log.debug("Git diff change count {}", gitDiffChanges.size());

        GitChangelistRef lastSubmittedChangelistInfo = git.lastSubmittedChangelistInfo();
        String versionToSyncTo = buildwebConfig.syncChangelistToLatestInBranch ? lastSubmittedChangelistInfo.getChangelistId() : "";
        perforce.syncPerforceFiles(gitDiffChanges, versionToSyncTo);

        Map<String, List<FileChange>> allPerforceChanges = perforce.getAllFileChangesInClient();
        boolean filesReverted = revertChangesNotInGitDiff(gitDiffChanges, allPerforceChanges);
        if (filesReverted) { // refech as files were reverted
            allPerforceChanges = perforce.getAllFileChangesInClient();
        }
        List<FileChange> changelistChanges = allPerforceChanges.get(draft.perforceChangelistId);
        List<FileChange> missingChanges = findChangesNotInPerforce(gitDiffChanges, allPerforceChanges);
        List<FileChange> allChangelistChanges = new ArrayList<>();
        if (changelistChanges != null) {
            allChangelistChanges.addAll(changelistChanges);
        }
        allChangelistChanges.addAll(missingChanges);

        List<FileChange> resyncedFiles = perforce.revertAndResyncUnresolvedFiles(allChangelistChanges, versionToSyncTo);
        missingChanges.addAll(resyncedFiles);
        perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, missingChanges);
        copyChangedFilesToClient(gitDiffChanges);
        perforce.renameAddOrDeleteFiles(draft.perforceChangelistId, missingChanges);
        long elapsedTime = new Date().getTime() - startingDate.getTime();
        long elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime);
        String plural = elapsedTimeInSeconds == 1 ? "" : "s";
        log.info("Synced changes to changelist {} in {} second{}\n", draft.perforceChangelistId,
                elapsedTimeInSeconds, plural);

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

            if (fileInGit.canExecute() == fileInPerforce.canExecute()) {
                continue;
            }
            log.debug("Changing file {} executable to {}", fileChange.getLastFileAffected(), fileInGit.canExecute());
            if (!fileInPerforce.setExecutable(fileInGit.canExecute())) {
                log.warn("Failed to set executable to {} for file {}", fileInGit.canExecute(), fileInPerforce.getPath());
            }
        }
    }

    private List<FileChange> findChangesNotInPerforce(List<FileChange> gitDiffChanges, Map<String, List<FileChange>> allPerforceChanges) {
        List<FileChange> changesToAddToPerforce = new ArrayList<>();
        List<FileChange> allPerforceChangesList = allChangesInMap(allPerforceChanges);
        for (FileChange gitDiffChange : gitDiffChanges) {
            if (!allPerforceChangesList.contains(gitDiffChange)) {
                changesToAddToPerforce.add(gitDiffChange);
                continue;
            }
            FileChange existingPerforceChange = allPerforceChangesList.get(allPerforceChangesList.indexOf(gitDiffChange));
            if (!existingPerforceChange.getPerforceChangelistId().equals(draft.perforceChangelistId)) {
                changesToAddToPerforce.add(existingPerforceChange);
            }
        }
        return changesToAddToPerforce;
    }

    private boolean revertChangesNotInGitDiff(List<FileChange> gitDiffChanges, Map<String, List<FileChange>> allPerforceChanges) {
        if (!allPerforceChanges.containsKey(draft.perforceChangelistId)) {
            return false;
        }

        List<FileChange> perforceChangesToRemove = new ArrayList<>(allPerforceChanges.get(draft.perforceChangelistId));
        perforceChangesToRemove.removeAll(gitDiffChanges);
        List<String> pathsToRevert = filePathsForChanges(perforceChangesToRemove);
        if (!pathsToRevert.isEmpty()) {
            log.info("Reverting following perforce changes not in git diff\n{}", pathsToRevert.toString());
            perforce.revertFiles(draft.perforceChangelistId, pathsToRevert);
            return true;
        }
        return false;
    }

    private List<String> filePathsForChanges(List<FileChange> changes) {
        List<String> filePaths = new ArrayList<>();
        for (FileChange change : changes) {
            String pathToUse = change.getLastFileAffected();
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
