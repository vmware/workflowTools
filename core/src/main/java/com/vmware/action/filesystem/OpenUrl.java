package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Open the specified url")
public class OpenUrl extends BaseAction {
    public OpenUrl(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceUrl");
    }

    @Override
    public void process() {
        SystemUtils.openUrl(fileSystemConfig.sourceUrl);
    }
}
