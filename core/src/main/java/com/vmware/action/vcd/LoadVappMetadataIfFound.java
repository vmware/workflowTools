package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Loads Vapp metadata value if found")
public class LoadVappMetadataIfFound extends BaseVappAction {
    public LoadVappMetadataIfFound(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("vappMetadataName");
        super.skipIfFileBasedVapp = true;
    }

    @Override
    public void process() {
        QueryResultVappType selectedVapp = vappData.getSelectedVapp();
        log.info("Loading metadata key {} for {}", vcdConfig.vappMetadataName, selectedVapp.name);
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = selectedVapp.getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");

        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        this.fileSystemConfig.fileData = metadata.getMetadata(vcdConfig.vappMetadataName);
        if (StringUtils.isEmpty(fileSystemConfig.fileData)) {
            log.info("No vapp metadata found for {}", vcdConfig.vappMetadataName);
        }
    }
}
