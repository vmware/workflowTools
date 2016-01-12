package com.vmware.action.conditional;

import com.vmware.action.base.BaseMultiActionDataSupport;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Helper action for exiting if there are no bugs to process.")
public class ExitIfThereAreNoBugsToProcess extends BaseMultiActionDataSupport {

    public ExitIfThereAreNoBugsToProcess(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (multiActionData.noBugsAdded()) {
            System.exit(0);
        }
    }
}
