package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Copies the testbed json for the selected Vapp to the clipboard.")
public class CopyVappJsonToClipboard extends BaseSingleVappJsonAction {
    public CopyVappJsonToClipboard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Copying testbed json for Vapp {} to clipboard", vappData.getSelectedVappName());
        SystemUtils.copyTextToClipboard(vappData.getJsonData());
    }
}
