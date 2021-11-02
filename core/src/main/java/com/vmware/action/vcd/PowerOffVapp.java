package com.vmware.action.vcd;

import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Powers off selected Vapp from Vcloud Director.")
public class PowerOffVapp extends BaseSingleVappAction {

    public PowerOffVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappType vappToPowerOff = vappData.getSelectedVapp();

        LinkType link = vappToPowerOff.getLinkByRel("power:powerOff");

        String confirmation = InputUtils.readValue("Confirm powering off of " + vappToPowerOff.name + ": Type yes to confirm");
        if (!confirmation.equalsIgnoreCase("yes")) {
            log.info("Aborting powering off of vapp {}", vappToPowerOff.name);
            return;
        }

        Vcd vcd = serviceLocator.getVcd();
        TaskType task = vcd.postResource(link, null);

        log.info("Task {} created for powering off vapp {}", task.href, vappToPowerOff.name);

        if (config.waitForBlockingWorkflowAction) {
            vcd.waitForTaskToComplete(task.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        }
    }
}
