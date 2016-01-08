package com.vmware.action.info;


import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays information about how api tokens are gathered.")
public class DisplayApiTokensInfo extends BaseAction {

    public DisplayApiTokensInfo(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Padder apiUsageTitle = new Padder("API Cookies / Tokens");

        apiUsageTitle.infoTitle();
        log.info("To interact with reviewboard, jira, jenkins and trello, you will be prompted to enter your username and password initially");
        log.info("");
        log.info("For Reviewboard, a long lived session cookie will be stored in [home folder]/.post-review-cookies.txt");
        log.info("If you are already using the post-review program, then that cookie will be reused.");
        log.info("");
        log.info("For Jira, authentication is done via the web Jira UI (not the REST API).");
        log.info("This is so the long lived remember me cookie can be retrieved. This is stored in [home folder]/.jira-cookies.txt");
        log.info("");
        log.info("For Jenkins, authentication is done via the web Jenkins UI and your API token scraped from your configure page");
        log.info("This is stored in [home folder]/.jenkins-api-token.txt");
        log.info("");
        log.info("For Trello, authentication is done via oauth with the Trello api");
        log.info("This is stored in [home folder]/.trello-api-token.txt");
        log.info("");
        log.info("For all subsequent api requests (until they expire), you will not need to enter your username and password");
        apiUsageTitle.infoTitle();
    }
}
