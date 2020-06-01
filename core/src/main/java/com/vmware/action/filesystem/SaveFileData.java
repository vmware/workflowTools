package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.base.BaseFileSystemAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;

@ActionDescription("Saves file data to a specified file. Source file path is used if destination file is not set or is a directory.")
public class SaveFileData extends BaseFileSystemAction {
    public SaveFileData(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("destinationFile");
    }

    @Override
    public void process() {
        log.info("Saving file data to {}", fileSystemConfig.destinationFile);
        IOUtils.write(new File(fileSystemConfig.destinationFile), fileData);
    }
}
