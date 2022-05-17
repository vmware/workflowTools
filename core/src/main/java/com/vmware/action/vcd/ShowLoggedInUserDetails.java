package com.vmware.action.vcd;

import java.util.Arrays;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.QuotaPools;
import com.vmware.vcd.domain.UserSession;
import com.vmware.vcd.domain.UserType;

@ActionDescription("Shows the logged in user full name and deployed vm quota")
public class ShowLoggedInUserDetails extends BaseAction {
    public ShowLoggedInUserDetails(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        UserType loggedInUser = serviceLocator.getVcd().getLoggedInUser();
        QuotaPools quotaPools = serviceLocator.getVcd().getQuotaPools(loggedInUser.id);
        if (quotaPools.quotaPools == null || quotaPools.quotaPools.length == 0) {
            log.info("{} - No Quotas", loggedInUser.fullName);
            return;
        }

        log.info(loggedInUser.name + " - " + loggedInUser.fullName);
        Arrays.stream(quotaPools.quotaPools).forEach(quotaPool ->log.info("{} {} used of {} {}",
                quotaPool.quotaPoolDefinition.quotaResourceName, quotaPool.quotaConsumed, quotaPool.quotaPoolDefinition.quota,
                quotaPool.quotaPoolDefinition.quotaResourceUnit));
    }
}
