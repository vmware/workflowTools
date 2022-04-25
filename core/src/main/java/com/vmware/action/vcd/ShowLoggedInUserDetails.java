package com.vmware.action.vcd;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.UserType;

@ActionDescription("Shows the logged in user full name and deployed vm quota")
public class ShowLoggedInUserDetails extends BaseAction {
    public ShowLoggedInUserDetails(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        UserType loggedInUser = serviceLocator.getVcd().getLoggedInUser();
        log.info("{} - max of {} deployed VMs", loggedInUser.fullName, loggedInUser.deployedVmQuota);
    }
}
