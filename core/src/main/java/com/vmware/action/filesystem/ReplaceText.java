package com.vmware.action.filesystem;

import com.vmware.action.base.BaseFileSystemAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Replaces all matches for a specified string in the file data using regex.")
public class ReplaceText extends BaseFileSystemAction {
    public ReplaceText(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("inputText", "replacementText");
    }

    @Override
    public void process() {
        log.info("Replacing text {} with {}", fileSystemConfig.inputText, fileSystemConfig.replacementText);
        fileData = fileData.replaceAll(fileSystemConfig.inputText, fileSystemConfig.replacementText);
    }
}
