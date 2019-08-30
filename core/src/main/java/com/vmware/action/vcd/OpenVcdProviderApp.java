package com.vmware.action.vcd;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import com.google.gson.Gson;
import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the endpoint specified in he Vapp Json")
public class OpenVcdProviderApp extends BaseSingleVappAction {
    public OpenVcdProviderApp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.vappJsonForJenkinsJob)) {
            return "no Vapp json loaded";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        log.info("Selected Vapp {}", vappData.getSelectedVapp());
        Gson gson = new ConfiguredGsonBuilder().build();
        Sites vcdSites = gson.fromJson(draft.vappJsonForJenkinsJob, Sites.class);
        String cellUrl = vcdSites.firstCellUrl() + "/provider";

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("Opening uri {}", cellUrl);
            try {
                Desktop.getDesktop().browse(URI.create(cellUrl));
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        } else {
            log.error("Cannot open url {} as BROWSER action for Java desktop is not supported", cellUrl);
        }
    }
}
