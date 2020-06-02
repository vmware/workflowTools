package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Opens the project documentation url if one is specified.")
public class OpenProjectDocumentationUrl extends BaseAction {

    public OpenProjectDocumentationUrl(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("projectDocumentationUrl");
    }

    @Override
    public void process() {
        SystemUtils.openUrl(config.projectDocumentationUrl);
    }
}
