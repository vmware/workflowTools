package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays initial info information")
public class GettingStarted extends AbstractAction {

    public GettingStarted(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Padder gettingStartedTitle = new Padder("Getting Started");
        gettingStartedTitle.infoTitle();
        log.info("This program utilizes the approach of workflows");
        log.info("A workflow consists of workflow actions");
        log.info("e.g. a workflow for making a git commit will consist of actions for setting the summary, description, bug number, testing done etc");

        log.info("");
        log.info("You can run a defined workflow");
        log.info("e.g. java -jar workflow.jar setTestingDone");
        log.info("or create a custom action list on the command list");
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
        log.info("");
        log.info("For a full listing of all workflow actions and configuration options");
        log.info("run java -jar workflow.jar help");
        gettingStartedTitle.infoTitle();

        Padder apiUsageTitle = new Padder("API Cookies / Tokens");

        apiUsageTitle.infoTitle();
        log.info("To interact with reviewboard, jira and jenkins, you will be prompted to enter your username and password initially");
        log.info("");
        log.info("For reviewboard, a long lived session cookie will be stored in [home folder]/.post-review-cookies.txt");
        log.info("If you are already using the post-review program, then that cookie will be reused.");
        log.info("");
        log.info("For Jira, authentication is done via the web Jira UI (not the REST API).");
        log.info("This is so the long lived remember me cookie can be retrieved. This is stored in [home folder]/.jira-cookies.txt");
        log.info("");
        log.info("For Jenkins, authentication is done via the web Jenkins UI and your API token scraped from your configure page");
        log.info("This is stored in [home folder]/.jenkins-api-token.txt");
        log.info("");
        log.info("For all subsequent api requests (until they expire), you will not need to enter your username and password");
        apiUsageTitle.infoTitle();
    }
}
