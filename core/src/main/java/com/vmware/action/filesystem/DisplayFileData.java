package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Displays file data")
public class DisplayFileData extends BaseAction {
    public DisplayFileData(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("fileData");
    }

    @Override
    public void process() {
        log.info("Loaded data: {}", fileSystemConfig.fileData);
    }
}
