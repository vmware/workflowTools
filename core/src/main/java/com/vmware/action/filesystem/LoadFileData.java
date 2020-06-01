package com.vmware.action.filesystem;

import com.vmware.action.base.BaseFileSystemAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;

@ActionDescription("Load the specified file into memory.")
public class LoadFileData extends BaseFileSystemAction {
    public LoadFileData(WorkflowConfig config) {
        super(config, false);
        super.addFailWorkflowIfBlankProperties("sourceFile");
    }

    @Override
    public void process() {
        log.info("Reading file {}", fileSystemConfig.sourceFile);
        fileData = IOUtils.read(fileSystemConfig.sourceFile);
    }
}
