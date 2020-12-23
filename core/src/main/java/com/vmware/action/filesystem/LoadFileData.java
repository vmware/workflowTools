package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;

@ActionDescription(value = "Load the specified file into memory.")
public class LoadFileData extends BaseAction {
    public LoadFileData(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile");
    }

    @Override
    public void process() {
        log.info("Reading file {}", fileSystemConfig.sourceFile);
        fileSystemConfig.fileData = IOUtils.read(fileSystemConfig.sourceFile);
    }
}
