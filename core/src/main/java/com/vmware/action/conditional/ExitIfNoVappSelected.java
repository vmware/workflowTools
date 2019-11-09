package com.vmware.action.conditional;

import com.vmware.action.base.BaseVappAction;
import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Exists if no Vapp has been selected.")
public class ExitIfNoVappSelected extends BaseVappAction {

    public ExitIfNoVappSelected(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (vappData.getSelectedVapp() == null) {
            exitWithMessage("no Vapp has been selected.");
        }
    }
}
