
package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.UrlUtils;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDataType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Saves vapp metadata value if a value is present")
public class SetVappMetadataIfNeeded extends BaseVappAction {
    public SetVappMetadataIfNeeded(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("vappMetadataName");
        super.addSkipActionIfBlankProperties("fileData");
    }

    @Override
    public void process() {
        QueryResultVappType selectedVapp = vappData.getSelectedVapp();
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = selectedVapp.getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");

        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        String existingValue = metadata.getMetadata(vcdConfig.vappMetadataName);

        if (fileSystemConfig.fileData.equals(existingValue)) {
            log.info("Vapp metadata {} has not changed, no need to update", vcdConfig.vappMetadataName);
            return;
        }

        MetaDataType updatedValue = new MetaDataType();
        updatedValue.key = vcdConfig.vappMetadataName;
        updatedValue.typedValue = new MetaDataType.TypedValue();
        updatedValue.typedValue.type = "MetadataStringValue";
        updatedValue.typedValue.value = fileSystemConfig.fileData;

        LinkType addLink = metadata.getLinkByRel("add");
        addLink.href = UrlUtils.addRelativePaths(addLink.href, vcdConfig.vappMetadataName);

        log.info("Updating vapp metadata {}", addLink.href);
        log.debug("Metadata value: {}", updatedValue.typedValue.value);
        vcd.updateResource(addLink, updatedValue);
    }
}
