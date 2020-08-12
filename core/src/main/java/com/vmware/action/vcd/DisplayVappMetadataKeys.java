package com.vmware.action.vcd;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Displays a list of the metadata keys for the Vapp.")
public class DisplayVappMetadataKeys extends BaseVappAction {
    public DisplayVappMetadataKeys(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappType selectedVapp = vappData.getSelectedVapp();
        log.info("Displaying metadata keys for {}", selectedVapp.name);
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = selectedVapp.getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");

        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        if (metadata.metadataEntry == null || metadata.metadataEntry.isEmpty()) {
            log.info("No metadata set");
            return;
        }
        List<String> keys = metadata.metadataEntry.stream().map(entry -> entry.key).collect(Collectors.toList());
        keys.forEach(key -> log.info(key));
    }
}
