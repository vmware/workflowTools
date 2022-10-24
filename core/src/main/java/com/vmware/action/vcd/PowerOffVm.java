package com.vmware.action.vcd;

import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Powers off selected Vm from Vcloud Director.")
public class PowerOffVm extends BaseSingleVappAction {

    public PowerOffVm(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVMType vmToPowerOff = selectQueryVmRecordFromVapp();
        LinkType link = vmToPowerOff.getLinkByRel("power:powerOff");
        TaskType task = serviceLocator.getVcd().postResource(link, null);

        log.info("Task {} created for powering off vm {}", task.href, vmToPowerOff.name);

        if (config.waitForBlockingWorkflowAction) {
            serviceLocator.getVcd().waitForTaskToComplete(task.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        }
    }
}
