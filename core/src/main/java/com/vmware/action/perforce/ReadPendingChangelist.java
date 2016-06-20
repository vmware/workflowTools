package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

@ActionDescription("Reads the most recent pending changelist for the specified user")
public class ReadPendingChangelist extends BaseCommitAction {

    public ReadPendingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistText = perforce.readLastPendingChangelist(config.username);
        if (StringUtils.isBlank(changelistText) || !changelistText.contains("\n")) {
            log.warn("No pending changelist exists for user {}", config.username);
            return;
        }
        String changelistId = MatcherUtils.singleMatch(changelistText, "Change\\s+(\\d+)\\s+on");
        if (changelistId == null) {
            throw new RuntimeException("Unable to parse changelist id from output\n" + changelistText);
        }
        log.info("Reading changelist {} as last pending changelist for user {}", changelistId, config.username);

        draft.perforceChangelistId = changelistId;
        String descriptionText = changelistText.substring(changelistText.indexOf('\n')).trim();
        draft.fillValuesFromCommitText(descriptionText, config.getCommitConfiguration());

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toGitText(config.getCommitConfiguration()));
        titlePadder.debugTitle();
    }
}
