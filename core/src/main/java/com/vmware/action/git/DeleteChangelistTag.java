package com.vmware.action.git;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Deletes matching changelist tag.")
public class DeleteChangelistTag extends BasePerforceCommitAction {

    @Override
    public String cannotRunAction() {
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        if (!git.listTags().contains(expectedTagName)) {
            return "tag " + expectedTagName + " does not exist";
        }
        return super.cannotRunAction();
    }

    public DeleteChangelistTag(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        log.info("Deleting tag {}", expectedTagName);
        git.deleteTag(expectedTagName);
    }
}
