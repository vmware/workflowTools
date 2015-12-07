package com.vmware.action.bugzilla;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractBatchIssuesAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueTypeDefinition;
import com.vmware.rest.UrlUtils;
import com.vmware.utils.input.InputUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Converts specified Bugzilla bug into a Jira issue.")
public class ConvertBugToIssue extends AbstractBatchIssuesAction{

    private Bugzilla bugzilla;

    public ConvertBugToIssue(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        bugzilla = ServiceLocator.getBugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug, true);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        int bugNumber = InputUtils.readValueUntilValidInt("Enter bugzilla bug number");

        Bug bug = bugzilla.getBugById(bugNumber);
        Issue matchingIssue = createIssueFromBug(bug);
        projectIssues.add(matchingIssue);

        log.info("Matching Jira Issue with summary {} will be created for bug {}", matchingIssue.getSummary(),
                matchingIssue.matchingBugzillaNumber(config.bugzillaUrl));
    }
}
