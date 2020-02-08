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
    public String cannotRunAction() {
        if (sshConfig.usesSshSite()) {
            return "ssh site is configured";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        if (vappData.noVappSelected()) {
            cancelWithMessage("no Vapp has been selected.");
        }
    }
}
