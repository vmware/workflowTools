package com.vmware.action.vcd;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CollectionUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.TaskType;

@ActionDescription("Action for bulk deleting of VMs")
public class DeleteVMs extends BaseAction {
    public DeleteVMs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Vcd vcd = serviceLocator.getVcd();
        QueryResultVMsType vmRecords = vcd.queryVms(vcdConfig.queryFilters());
        if (CollectionUtils.isEmpty(vmRecords.record)) {
            log.info("No VMs found");
            return;
        }
        List<String> choices = vmRecords.record.stream().map(QueryResultVMType::getLabel).collect(Collectors.toList());
        List<Integer> vmIndexes = InputUtils.readSelections(choices, "Select VMs to delete", false);

        String vmsToDelete = vmIndexes.stream().map(choices::get).collect(Collectors.joining(","));
        log.info("VMs {} will be deleted", vmsToDelete);
        String confirmation = InputUtils.readValueUntilNotBlank("Delete (Y/N)");
        if ("Y".equalsIgnoreCase(confirmation)) {
            vmIndexes.stream().map(i -> vmRecords.record.get(i)).forEach(vm -> {
                log.info("Deleting VM {}", vm.getLabel());
                TaskType deleteTask = serviceLocator.getVcd().deleteResource(vm.getSelfLink(), true);
                vcd.waitForTaskToComplete(deleteTask.href, 1, TimeUnit.MINUTES);
            });
        }
    }
}
