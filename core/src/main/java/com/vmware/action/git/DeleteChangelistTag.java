package com.vmware.action.git;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Deletes matching changelist tag.")
public class DeleteChangelistTag extends BaseLinkedPerforceCommitUsingGitAction {

    public DeleteChangelistTag(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        super.skipActionIfTrue(!git.listTags().contains(expectedTagName), "tag " + expectedTagName + " does not exist");
    }

    @Override
    public void process() {
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        log.info("Deleting tag {}", expectedTagName);
        git.deleteTag(expectedTagName);
    }
}
