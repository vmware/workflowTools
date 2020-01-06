package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseSingleVappAction extends BaseVappAction {

    @Override
    public String cannotRunAction() {
        if (vappData.noVappSelected()) {
            return "no Vapp has been selected";
        }
        return super.cannotRunAction();
    }

    public BaseSingleVappAction(WorkflowConfig config) {
        super(config);
    }
}
