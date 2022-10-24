package com.vmware.action.vcd;

import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Powers on selected Vm from Vcloud Director.")
public class PowerOnVm extends BaseSingleVappAction {

    public PowerOnVm(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVMType vmToPowerOn = selectQueryVmRecordFromVapp();
        LinkType link = vmToPowerOn.getLinkByRel("power:powerOn");
        TaskType task = serviceLocator.getVcd().postResource(link, null);

        log.info("Task {} created for powering on vm {}", task.href, vmToPowerOn.name);

        if (config.waitForBlockingWorkflowAction) {
            serviceLocator.getVcd().waitForTaskToComplete(task.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        }
    }
}
