package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractReadMultiLine;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Append text to the existing testing done section.")
public class AddToTestingDone extends AbstractReadMultiLine {

    public AddToTestingDone(WorkflowConfig config) throws NoSuchFieldException {
        super(config, "testingDone", true);
    }

}
