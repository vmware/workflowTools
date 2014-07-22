package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Displays the commit message for the last commit on the console.")
public class DisplayLastCommit extends AbstractAction {

    public DisplayLastCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        Padder titlePadder = new Padder("Last Commit");
        titlePadder.infoTitle();
        log.info(git.lastCommitText(false));
        titlePadder.infoTitle();
    }
}
