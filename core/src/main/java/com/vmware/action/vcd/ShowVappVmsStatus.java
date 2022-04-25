package com.vmware.action.vcd;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.QueryResultVMsType;

@ActionDescription("Shows VM name and power status for each vm in the selected Vapp")
public class ShowVappVmsStatus extends BaseSingleVappAction {
    public ShowVappVmsStatus(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Showing VMs for Vapp {}", vappData.getSelectedVapp().name);
        QueryResultVMsType vmsForVapp = serviceLocator.getVcd().queryVmsForVapp(vappData.getSelectedVapp().parseIdFromRef());
        List<QueryResultVMType> vmsSortedByStatusAndName = vmsForVapp.record.stream().sorted(Comparator.comparing(QueryResultVMType::getStatus)
                .thenComparing(QueryResultVMType::getName)).collect(Collectors.toList());
        vmsSortedByStatusAndName.forEach(vm -> log.info("{} {}", vm.name, vm.status));
    }
}
