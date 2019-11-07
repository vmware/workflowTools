package com.vmware.action.vcd;

import com.google.gson.Gson;
import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.BrowserUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the specified vcd provider endpoint in the Vapp Json")
public class OpenVcdProviderApp extends BaseSingleVappJsonAction {
    public OpenVcdProviderApp(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    public void process() {
        String uiUrl = uiUrl() + "/provider";
        BrowserUtils.openUrl(uiUrl);
    }

    protected String uiUrl() {
        Sites.Site selectedSite = vappData.getSelectedSite();
        if (selectedSite.loadBalancer != null) {
            log.info("Using loadbalancer url {}", selectedSite.loadBalancer.endPointURI);
            return selectedSite.loadBalancer.endPointURI;
        }
        if (vappData.getSelectedVcdCell() == null) {
            selectVcdCell(selectedSite, vcdConfig.vcdCellIndex);
        }
        Sites.DeployedVM selectedCell = vappData.getSelectedVcdCell();
        return selectedCell.endPointURI;
    }
}
