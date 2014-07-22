package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays the commit details in memory.")
public class DisplayCommit extends AbstractCommitAction {

    public DisplayCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Padder titlePadder = new Padder("Commit Details");
        titlePadder.infoTitle();
        log.info(draft.toGitText(config.getCommitConfiguration()));
        titlePadder.infoTitle();
    }
}
