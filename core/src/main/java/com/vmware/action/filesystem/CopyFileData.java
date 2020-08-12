package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Copies file data to clipboard")
public class CopyFileData extends BaseAction {
    public CopyFileData(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("fileData");
    }

    @Override
    public void process() {
        log.info("Copying data to clipboard");
        log.trace("Loaded data: {}", fileSystemConfig.fileData);
        SystemUtils.copyTextToClipboard(fileSystemConfig.fileData);
    }
}
