package com.vmware.action.conditional;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exists if no Vapp has been selected.")
public class ExitIfNoVappSelected extends BaseVappAction {

    public ExitIfNoVappSelected(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(sshConfig.usesSshSite(), "ssh site is configured");
    }

    @Override
    public void process() {
        if (vappData.noVappSelected()) {
            cancelWithMessage("no Vapp has been selected.");
        }
    }
}
