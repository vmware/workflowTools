package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.List;

@ActionDescription("Used to revert all pending changelists in a perforce workspace.")
public class RevertWorkspace extends BaseCommitAction {
    public RevertWorkspace(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<String> changelistIds = perforce.getPendingChangelists(config.perforceClientName);

        for (String changelistId : changelistIds) {
            revertChangesInChangelist(changelistId);
        }

        log.info("Reverting open files in default changelist for client {}", config.perforceClientName);
        perforce.revertChangesInPendingChangelist("default");
    }

    private void revertChangesInChangelist(String changelistId) {
        log.info("Reverting open files in changelist {} for client {}", changelistId, config.perforceClientName);
        perforce.revertChangesInPendingChangelist(changelistId);
        List<String> tags = git.listTags();
        if (changelistId.equals(draft.perforceChangelistId)) {
            log.info("Not deleting changelist {} as it matches the current commit", changelistId);
        } else {
            log.info("Deleting pending changelist {} in client {}", changelistId, config.perforceClientName);
            perforce.deletePendingChangelist(changelistId);
            deleteMatchingChangelistTag(tags, changelistId);
        }
    }

    private void deleteMatchingChangelistTag(List<String> tags, String changelistId) {
        String matchingTag = "changeset-" + changelistId;
        if (tags.contains(matchingTag)) {
            git.deleteTag(matchingTag);
        }
    }


}
