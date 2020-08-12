package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Deletes vapp metadata value if a value is present")
public class DeleteVappMetadataIfNeeded extends BaseVappAction {
    public DeleteVappMetadataIfNeeded(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("vappMetadataName");
    }

    @Override
    public void process() {
        QueryResultVappType selectedVapp = vappData.getSelectedVapp();
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = selectedVapp.getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");

        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        String existingValue = metadata.getMetadata(vcdConfig.vappMetadataName);

        if (StringUtils.isEmpty(existingValue)) {
            log.info("No metadata value found for {}", vcdConfig.vappMetadataName);
        }

        log.debug("Metadata value: {}", existingValue);
        LinkType deleteLink = metadata.getLinkByRel("add");
        deleteLink.href = UrlUtils.addRelativePaths(deleteLink.href, vcdConfig.vappMetadataName);

        log.info("Deleting vapp metadata {}", deleteLink.href);
        vcd.deleteResource(deleteLink, false);
    }
}
