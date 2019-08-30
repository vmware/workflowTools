package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;

@ActionDescription("Loads Json metadata for selected Vapp")
public class LoadVappJson extends BaseSingleVappAction {
    public LoadVappJson(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = vappData.getSelectedVapp().getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");
        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        draft.vappJsonForJenkinsJob = metadata.jsonMetadata();
    }
}
