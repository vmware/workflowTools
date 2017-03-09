package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.List;

@ActionDescription("Used to revert all pending changelists in a perforce workspace.")
public class RevertWorkspace extends BasePerforceCommitAction {
    public RevertWorkspace(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<String> changelistIds = perforce.getPendingChangelists();

        for (String changelistId : changelistIds) {
            revertChangesInChangelist(changelistId);
        }

        log.info("Reverting open files in default changelist for client {}", config.perforceClientName);
        perforce.revertChangesInPendingChangelist("default");
    }

    private void revertChangesInChangelist(String changelistId) {
        log.info("Reverting open files in changelist {} for client {}", changelistId, config.perforceClientName);
        perforce.revertChangesInPendingChangelist(changelistId);
        if (changelistId.equals(draft.perforceChangelistId)) {
            log.info("Not deleting changelist {} as it matches the current commit", changelistId);
        } else {
            log.info("Deleting pending changelist {} in client {}", changelistId, config.perforceClientName);
            perforce.deletePendingChangelist(changelistId);
            deleteMatchingChangelistTag(changelistId);
        }
    }

    private void deleteMatchingChangelistTag(String changelistId) {
        if (!git.workingDirectoryIsInGitRepo()) {
            return;
        }
        List<String> tags = git.listTags();
        String matchingTag = "changeset-" + changelistId;
        if (tags.contains(matchingTag)) {
            git.deleteTag(matchingTag);
        }
    }


}
