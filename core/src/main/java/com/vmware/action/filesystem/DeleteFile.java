package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Deletes the specified file.")
public class DeleteFile extends BaseAction {
    public DeleteFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile");
    }

    @Override
    public void process() {
        log.info("Deleting file {}", fileSystemConfig.sourceFile);
        File sourceFile = new File(fileSystemConfig.sourceFile);
        boolean fileDeleted = sourceFile.delete();
        if (!fileDeleted) {
            log.warn("File {} might not be deleted!", sourceFile.getAbsolutePath());
        }
    }
}
