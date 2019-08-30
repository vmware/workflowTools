package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.VappData;

public abstract class BaseVappAction extends BaseCommitAction {
    protected VappData vappData;

    public BaseVappAction(WorkflowConfig config) {
        super(config);
    }

    public void setVappData(VappData vappData) {
        this.vappData = vappData;
    }
}
