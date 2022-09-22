package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.vcd.Vcd;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Loads Json metadata for selected Vapp")
public class LoadVappJson extends BaseSingleVappAction {
    public LoadVappJson(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        QueryResultVappType selectedVapp = vappData.getSelectedVapp();
        if (selectedVapp.jsonDataLoaded()) {
            log.debug("Vapp json has already been loaded for selected Vapp {}", selectedVapp.name);
            return;
        }
        if (selectedVapp.isJsonFileBased()) {
            throw new FatalException("Json data should already be loaded for file based Vapp {}", selectedVapp.name);
        }
        Vcd vcd = serviceLocator.getVcd();
        LinkType metadataLink = selectedVapp.getLinkByRelAndType("down", "application/vnd.vmware.vcloud.metadata+xml");
        MetaDatasType metadata = vcd.getVappMetaData(metadataLink);
        fileSystemConfig.fileData = metadata.jsonMetadata();
        if (StringUtils.isNotBlank(fileSystemConfig.fileData)) {
            selectedVapp.parseJson(fileSystemConfig.fileData);
        } else {
            log.info("Failed to find json metadata for vapp {}, constructing site from Vapp VM listing", selectedVapp.name);
            QueryResultVMsType vMsType = vcd.queryVmsForVapp(selectedVapp.parseIdFromRef());
            selectedVapp.createSiteFromVms(vMsType);
        }

    }
}
