package com.vmware.action.git;

import java.util.List;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Updates matching changelist tag if not set or revision is different.")
public class UpdateChangelistTag extends BaseLinkedPerforceCommitUsingGitAction {

    public UpdateChangelistTag(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String expectedTagName = "changeset-" + draft.perforceChangelistId;
        List<String> existingTags = git.listTags();
        if (!existingTags.contains(expectedTagName)) {
            log.info("Creating tag {}", expectedTagName);
            git.updateTag(expectedTagName, LogLevel.DEBUG);
        } else {
            String existingRevision = git.revParse(expectedTagName);
            String headRevision = git.revParse("head");
            if (!headRevision.equals(existingRevision)) {
                log.info("Updating {} from {} revision to {}", expectedTagName, existingRevision, headRevision);
                git.updateTag(expectedTagName, LogLevel.DEBUG);
            } else {
                log.debug("{} already matches head revision", expectedTagName, headRevision);
            }
        }
    }
}
