package com.vmware.action.jira;

import com.vmware.action.BaseAction;
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
public class UpdateTaskEstimates extends BaseAction {

    private Jira jira;

    public UpdateTaskEstimates(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        this.jira = serviceLocator.getJira();
    }

    @Override
    public void preprocess() {
        this.jira.setupAuthenticatedConnection();
    }

    @Override
    public void process() {
        log.info("Updating open jira issues with no original estimate to {} hour(s)", config.jiraTaskEstimateInHours);
        updateEstimatesIfNeeded(jira.getCreatedTasksForUser(), "Created Tasks");
        updateEstimatesIfNeeded(jira.getOpenTasksForUser(), "Assigned Tasks");
    }

    private void updateEstimatesIfNeeded(IssuesResponse tasks, String title) {
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
