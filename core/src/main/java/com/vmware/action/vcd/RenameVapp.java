package com.vmware.action.vcd;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.TaskType;
import com.vmware.vcd.domain.VappType;

@ActionDescription("Rename a Vapp")
public class RenameVapp extends BaseSingleVappAction {
    public RenameVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String newName = InputUtils.readValueUntilNotBlank("Enter new Vapp Name");

        VappType vappTypeForUpdate = new VappType();
        vappTypeForUpdate.name = newName;
        vappTypeForUpdate.description = "Updated by workflows tools on " + new Date().toString();

        Vcd vcd = serviceLocator.getVcd();
        TaskType updatedVappTask = vcd.updateResource(vappData.getSelectedVapp().href, vappTypeForUpdate);
        vcd.waitForTaskToComplete(updatedVappTask.href, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        vappData.getSelectedVapp().name = newName;
    }
}
