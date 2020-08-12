package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;

@ActionDescription("Appends the specified test to the source file.")
public class AppendTextToFile extends BaseAction {
    public AppendTextToFile(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Appending following to file {}\n{}", fileSystemConfig.sourceFile, fileSystemConfig.inputText);
        IOUtils.appendToFile(new File(fileSystemConfig.sourceFile), fileSystemConfig.inputText);
    }
}
