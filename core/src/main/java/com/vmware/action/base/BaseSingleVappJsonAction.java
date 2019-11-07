package com.vmware.action.base;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

public abstract class BaseSingleVappJsonAction extends BaseSingleVappAction {

    public BaseSingleVappJsonAction(WorkflowConfig config) {
        super(config);
        super.checkVappJson = true;
    }

    protected void validateListSelection(List values, String propertyName, int selection) {
        if (selection < 1 || selection > values.size()) {
            throw new FatalException("{} selection {} is invalid. Should be between 1 and {}",
                    propertyName, String.valueOf(selection), String.valueOf(values.size()));
        }
    }

    protected void selectVcdCell(Sites.Site site, Integer cellIndex) {
        if (cellIndex == null && site.cells.size() == 1) {
            log.info("Using first cell as there is only one cell");
            vappData.setSelectedVcdCell(site.cells.get(0));
        } else if (cellIndex != null) {
            log.info("Using specified cell index of {}", cellIndex);
            validateListSelection(site.cells, "vcd cell index", cellIndex);
            vappData.setSelectedVcdCell(site.cells.get(cellIndex - 1));
        } else if (vappData.getSelectedVcdCell() != null) {
            log.info("Using already selected cell {}", vappData.getSelectedVcdCell().name);
        } else {
            List<String> cellChoices = site.cells.stream().map(cell -> cell.name).collect(Collectors.toList());
            int index = InputUtils.readSelection(cellChoices, "Select Cell");
            vappData.setSelectedVcdCell(site.cells.get(index));
        }

    }
}
