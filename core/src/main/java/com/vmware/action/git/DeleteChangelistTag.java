package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Deletes matching changelist tag.")
public class DeleteChangelistTag extends BaseLinkedPerforceCommitUsingGitAction {

    public DeleteChangelistTag(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        String actionCannotBeRun = super.cannotRunAction();
        if (StringUtils.isNotEmpty(actionCannotBeRun)) {
            return actionCannotBeRun;
        }
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        if (!git.listTags().contains(expectedTagName)) {
            return "tag " + expectedTagName + " does not exist";
        }
        return null;
    }

    @Override
    public void process() {
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        log.info("Deleting tag {}", expectedTagName);
        git.deleteTag(expectedTagName);
    }
}
