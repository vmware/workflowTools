package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.QueryResultVMType;
import com.vmware.vcd.domain.QueryResultVMsType;

import java.util.stream.Collectors;

public abstract class BaseSingleVappAction extends BaseVappAction {

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(vappData.noVappSelected(), "no Vapp has been selected");
    }

    public BaseSingleVappAction(WorkflowConfig config) {
        super(config);
    }

    protected QueryResultVMType selectQueryVmRecordFromVapp() {
        QueryResultVMsType queryVMs = serviceLocator.getVcd().queryVmsForVapp(vappData.getSelectedVapp().parseIdFromRef());

        int selection = InputUtils.readSelection(queryVMs.record.stream().map(InputListSelection.class::cast).collect(Collectors.toList()), "Select VM");
        return queryVMs.record.get(selection);
    }
}
