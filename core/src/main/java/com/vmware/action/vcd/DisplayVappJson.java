package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

public class DisplayVappJson extends BaseSingleVappJsonAction {
    public DisplayVappJson(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder jsonPadder = new Padder(vappData.getSelectedVapp().name);
        jsonPadder.infoTitle();
        log.info(draft.vappJsonForJenkinsJob);
        jsonPadder.infoTitle();
    }
}
