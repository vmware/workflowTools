package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

@ActionDescription("Displays the testbed json for the selected Vapp.")
public class DisplayVappJson extends BaseSingleVappJsonAction {
    public DisplayVappJson(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder jsonPadder = new Padder(vappData.getSelectedVappName());
        jsonPadder.infoTitle();
        log.info(vappData.getJsonData());
        jsonPadder.infoTitle();
    }
}
