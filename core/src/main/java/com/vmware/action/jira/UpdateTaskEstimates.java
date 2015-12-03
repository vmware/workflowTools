package com.vmware.action.jira;

import com.vmware.ServiceLocator;
import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssuesResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

@ActionDescription("Convenience operation for batch updating the estimate value for all jira tasks reported by or assigned to the configured user.")
public class UpdateTaskEstimates extends AbstractAction {

    private Jira jira;

    public UpdateTaskEstimates(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jira = ServiceLocator.getJira(config.jiraUrl, true);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("Updating open jira issues with no original estimate to {} hour(s)", config.jiraTaskEstimateInHours);
        updateEstimatesIfNeeded(jira.getCreatedTasksForUser(config.username), "Created Tasks");
        updateEstimatesIfNeeded(jira.getOpenTasksForUser(config.username), "Assigned Tasks");
    }

    private void updateEstimatesIfNeeded(IssuesResponse tasks, String title) throws IllegalAccessException, IOException, URISyntaxException {
        log.info("Checking {}", title);
        for (Issue task : tasks.issues) {
            log.info("");
            log.info("Checking issue {} ({})", task.getKey(), task.fields.summary);
            if (task.fields.originalEstimateInSeconds > 0) {
                log.info("Issue already has an estimate value of {} hour(s) so skipping",
                        HOURS.convert(task.fields.originalEstimateInSeconds, SECONDS));
                continue;
            }
            log.info("Updating issue's original estimate to {} hour(s)", config.jiraTaskEstimateInHours);
            jira.updateIssueEstimate(task.getKey(), config.jiraTaskEstimateInHours);
            log.info("Successfully updated jira issue");
        }
        log.info("");
    }
}
