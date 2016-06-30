package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

@ActionDescription("Reads the most recent pending changelist for the specified perforce client.")
public class ReadPendingChangelist extends BaseCommitAction {

    public ReadPendingChangelist(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public void process() {
        String changelistText = perforce.readLastPendingChangelist(config.perforceClientName);
        if (StringUtils.isBlank(changelistText) || !changelistText.contains("\n")) {
            log.warn("No pending changelist exists for user {}", config.username);
            return;
        }

        draft.fillValuesFromCommitText(changelistText, config.getCommitConfiguration());
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            throw new RuntimeException("Failed to parse changelist id from text\n" + changelistText);
        }

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toGitText(config.getCommitConfiguration()));
        titlePadder.debugTitle();
    }
}
