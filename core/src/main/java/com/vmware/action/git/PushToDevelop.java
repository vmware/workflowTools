package com.vmware.action.git;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:develop.")
public class PushToDevelop extends AbstractAction {

    public PushToDevelop(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        git.pushToRemoteBranch("develop");
    }
}
