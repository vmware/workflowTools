package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseSingleVappAction extends BaseVappAction {

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(vappData.noVappSelected(), "no Vapp has been selected");
    }

    public BaseSingleVappAction(WorkflowConfig config) {
        super(config);
    }
}
