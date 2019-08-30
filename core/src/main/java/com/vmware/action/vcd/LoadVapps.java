package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.QueryResultVappsType;


@ActionDescription("Loads a list of vapps from Vcloud Director owned by the logged in user.")
public class LoadVapps extends BaseVappAction {
    public LoadVapps(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappsType vappRecords = serviceLocator.getVcd().getVapps();
        vappData.setOwnedVapps(vappRecords.record);
    }
}
