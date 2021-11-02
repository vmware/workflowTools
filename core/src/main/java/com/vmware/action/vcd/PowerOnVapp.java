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

@ActionDescription("Powers on selected Vapp from Vcloud Director.")
public class PowerOnVapp extends BaseSingleVappAction {

    public PowerOnVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappType vappToPowerOn = vappData.getSelectedVapp();

        LinkType link = vappToPowerOn.getLinkByRel("power:powerOn");
        Vcd vcd = serviceLocator.getVcd();
        TaskType task = vcd.postResource(link, null);

        log.info("Task {} created for powering on vapp {}", task.href, vappToPowerOn.name);

        if (config.waitForBlockingWorkflowAction) {
            vcd.waitForTaskToComplete(task.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        }
    }
}
