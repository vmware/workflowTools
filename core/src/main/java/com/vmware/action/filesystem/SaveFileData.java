package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;

@ActionDescription("Saves file data to a specified file.")
public class SaveFileData extends BaseAction {
    public SaveFileData(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("destinationFile", "fileData");
    }

    @Override
    public void process() {
        log.info("Saving to {}", fileSystemConfig.destinationFile);
        IOUtils.write(new File(fileSystemConfig.destinationFile), fileSystemConfig.fileData);
    }
}
