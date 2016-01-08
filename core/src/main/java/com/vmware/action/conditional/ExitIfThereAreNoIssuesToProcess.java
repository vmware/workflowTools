package com.vmware.action.conditional;

import com.vmware.action.base.BaseMultiActionDataSupport;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Helper action for exiting if there are no project issues to process.")
public class ExitIfThereAreNoIssuesToProcess extends BaseMultiActionDataSupport {

    public ExitIfThereAreNoIssuesToProcess(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (multiActionData.noIssuesAdded()) {
            System.exit(0);
        }
    }
}
