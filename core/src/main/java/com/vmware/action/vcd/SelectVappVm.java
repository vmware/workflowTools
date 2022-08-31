package com.vmware.action.vcd;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription(value = "Selects a Vapp VM.",
        configFlagsToAlwaysExcludeFromCompleter = {"--ssh-site", "--ssh-host", "--ssh-port", "--ssh-username", "--ssh-password", "--ssh-strict-host-checking"})
public class SelectVappVm extends BaseSingleVappJsonAction {
    public SelectVappVm(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<Sites.VmInfo> vms = vappData.getSelectedSite().vms();
        if (StringUtils.isNotBlank(vcdConfig.vmName)) {
            log.info("Using specified VM name {}", vcdConfig.vmName);
            List<String> vmNames = vms.stream().map(Sites.VmInfo::getName).collect(Collectors.toList());
            Sites.VmInfo matchingVm = vms.stream().filter(vm -> vm.getName().equals(vcdConfig.vmName))
                    .findFirst().orElseThrow(() -> new FatalException("No VM named " + vcdConfig.vmName + " in " + vmNames));
            vappData.setSelectedVm(matchingVm);
        } else {
            int selectedVmIndex = InputUtils.readSelection(vms.stream().map(Sites.VmInfo::getLabel).collect(Collectors.toList()), "Select VM");
            vappData.setSelectedVm(vms.get(selectedVmIndex));
        }
    }
}
