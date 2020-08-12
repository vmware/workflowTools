package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Replaces all matches for a specified string in the file data using regex.")
public class ReplaceText extends BaseAction {
    public ReplaceText(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "inputText", "replacementText");
    }

    @Override
    public void process() {
        log.info("Replacing text {} with {}", fileSystemConfig.inputText, fileSystemConfig.replacementText);
        fileSystemConfig.fileData = fileSystemConfig.fileData.replaceAll(fileSystemConfig.inputText, fileSystemConfig.replacementText);
    }
}
