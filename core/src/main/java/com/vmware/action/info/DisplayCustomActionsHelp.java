package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays help messaging pertaining to running a custom action list.")
public class DisplayCustomActionsHelp extends BaseAction {


    public DisplayCustomActionsHelp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder padder = new Padder("Custom Actions List");

        padder.infoTitle();
        log.info("The normal approach would be to run a predefined workflow");
        log.info("e.g. java -jar workflow.jar setTestingDone");
        log.info("Alternatively you can run a custom action via the command line");
        log.info("e.g. java -jar workflow.jar ReadLastCommit,SetTestingDone,AmendCommit,MarkIssueAsInProgress");

        log.info("");
        log.info("N.B. If creating a custom action list, AmendCommit, AmendCommitAll, Commit or CommitAll");
        log.info("MUST be used to save any changes to the commit");

        log.info("");
        log.info("Additionally workflow names can be used in a custom action list as well");
        log.info("e.g. java -jar workflow.jar setTestingDone,MarkIssueAsInProgress");

        log.info("");
        log.info("In general, workflow names are title cased and action names are capitalized");
        log.info("To see what actions will be run, use the flag -dr to do a dry run");
        padder.infoTitle();
    }
}
