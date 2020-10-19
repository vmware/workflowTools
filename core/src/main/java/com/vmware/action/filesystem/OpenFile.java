package com.vmware.action.filesystem;

import java.io.File;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Open the specified file")
public class OpenFile extends BaseAction {
    public OpenFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("destinationFile");
    }

    @Override
    public void process() {
        SystemUtils.openUrl(new File(fileSystemConfig.destinationFile).getAbsolutePath());
    }
}
