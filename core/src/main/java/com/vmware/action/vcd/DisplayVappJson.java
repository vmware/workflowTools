package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

public class DisplayVappJson extends BaseSingleVappAction {
    public DisplayVappJson(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.vappJsonForJenkinsJob)) {
            return "no Vapp json loaded";
        }
        return super.failWorkflowIfConditionNotMet();
    }


    @Override
    public void process() {
        Padder jsonPadder = new Padder(vappData.getSelectedVapp().name);
        jsonPadder.infoTitle();
        log.info(draft.vappJsonForJenkinsJob);
        jsonPadder.infoTitle();
    }
}
