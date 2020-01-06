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

@ActionDescription("Delete selected Vapp from Vcloud Director.")
public class DeleteVapp extends BaseSingleVappAction {

    public DeleteVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappType vappToDelete = vappData.getSelectedVapp();
        String confirmation = InputUtils.readValue("Confirm deletion of " + vappToDelete.name + ": Type yes to confirm");
        if (!confirmation.equalsIgnoreCase("yes")) {
            log.info("Aborting deletion of vapp {}", vappToDelete.name);
        }

        LinkType deleteLink = vappToDelete.getSelfLink();
        Vcd vcd = serviceLocator.getVcd();
        TaskType deleteTask = vcd.deleteResource(deleteLink, true);

        log.info("Task {} created for deleting vapp {}", deleteTask.href, vappToDelete.name);

        if (config.waitForBlockingWorkflowAction) {
            vcd.waitForTaskToComplete(deleteTask.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        }
    }
}
