package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Simple action for displaying project documentation url")
public class DisplayProjectDocumentationUrl extends BaseAction {
    public DisplayProjectDocumentationUrl(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isNotBlank(config.projectDocumentationUrl)) {
            log.info("Project documentation: " + config.projectDocumentationUrl);
        } else {
            log.debug("No project documentation url specified");
        }
    }
}
