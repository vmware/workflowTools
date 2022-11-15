package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.exception.RuntimeIOException;

import java.io.FileNotFoundException;

@ActionDescription(value = "Load the specified file into memory as a variable.")
public class LoadFileAsVariable extends BaseAction {
    public LoadFileAsVariable(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceFile", "outputVariableName");
    }

    @Override
    public void process() {
        log.info("Reading file {} as variable {}", fileSystemConfig.sourceFile, fileSystemConfig.outputVariableName);
        try {
            String fileData = IOUtils.read(fileSystemConfig.sourceFile);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, fileData);
        } catch (RuntimeIOException io) {
            if (io.getCause() instanceof FileNotFoundException) {
                log.info("File {} was not found, not setting variable {}", fileSystemConfig.sourceFile, fileSystemConfig.outputVariableName);
            } else {
                throw io;
            }
        }
    }
}
