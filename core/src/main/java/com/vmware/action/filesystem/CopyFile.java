package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;

import java.io.File;

@ActionDescription("Copies file / directory to the destination file / directory")
public class CopyFile extends BaseAction {
    public CopyFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "destinationFile");
    }

    @Override
    public void process() {
        log.info("Copying {} to {}", fileSystemConfig.sourceFile, fileSystemConfig.destinationFile);
        File sourceFile = new File(fileSystemConfig.sourceFile);
        if (sourceFile.isDirectory()) {
            FileUtils.copyDirectory(sourceFile, new File(fileSystemConfig.destinationFile));
        } else {
            FileUtils.copyFile(sourceFile, new File(fileSystemConfig.destinationFile));
        }
    }
}
