package com.vmware.action.vcd;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Selects a VCD site.")
public class SelectVcdSite extends BaseSingleVappJsonAction {
    public SelectVcdSite(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (vcdConfig.vcdSiteIndex == null && vappData.getSelectedSite() != null) {
            log.info("Using already selected site");
            return;
        }
        log.info("Selected Vapp {}", vappData.getSelectedVappName());
        vappData.setSelectedSite(determineSite(vappData.getSelectedVapp().getVcdSites(), vcdConfig.vcdSiteIndex));
    }

    private Sites.Site determineSite(List<Sites.Site> sites, Integer siteIndex) {
        if (siteIndex == null && sites.size() == 1) {
            log.info("Using first site as there is only one site");
            siteIndex = 0;
        } else if (siteIndex == null) {
            siteIndex = InputUtils.readSelection(IntStream.range(0, sites.size()).mapToObj(String::valueOf).collect(Collectors.toList()), "Select Site");
        } else {
            log.info("Using specified site index of {}", siteIndex);
            validateListSelection(sites, "vcd site index", siteIndex);
            siteIndex--; // subtract one to match zero indexed list
        }
        return sites.get(siteIndex);
    }
}
