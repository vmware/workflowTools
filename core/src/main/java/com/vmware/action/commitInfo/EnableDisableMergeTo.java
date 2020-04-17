package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.Arrays;

@ActionDescription("Enables or disables the merge to values for the commit")
public class EnableDisableMergeTo extends BaseCommitAction {
    public EnableDisableMergeTo(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.mergeToValues.length == 0, "no merge to values set for this commit");
    }

    @Override
    public void process() {
        if (commitConfig.disableMergeTo) {
            log.info("Disabling merge to for commit");
        } else {
            log.info("Enabling merge to for commit");
        }
        log.debug("Values to disable {}", Arrays.toString(draft.mergeToValues));

        for (int i = 0; i < draft.mergeToValues.length; i++ ) {
            String mergeToValue = draft.mergeToValues[i];
            if (commitConfig.disableMergeTo) {
                mergeToValue = mergeToValue.replace(": yes", ": no");
                mergeToValue = mergeToValue.replace(": Yes", ": No");
                mergeToValue = mergeToValue.replace(": YES", ": NO");
            } else {
                mergeToValue = mergeToValue.replace(": no", ": yes");
                mergeToValue = mergeToValue.replace(": No", ": Yes");
                mergeToValue = mergeToValue.replace(": NO", ": YES");
            }
            draft.mergeToValues[i] = mergeToValue;
            log.info(mergeToValue);
        }
    }
}
