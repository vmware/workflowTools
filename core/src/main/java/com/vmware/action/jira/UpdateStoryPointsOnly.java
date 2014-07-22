package com.vmware.action.jira;

import com.vmware.action.base.AbstractBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.rest.exception.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Bulk update the story points for jira issues")
public class UpdateStoryPointsOnly extends AbstractBatchJiraAction {

    public UpdateStoryPointsOnly(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (projectIssues.getIssuesFromJira().isEmpty()) {
            log.info("Skipping {} as there are no issues loaded from Jira.", this.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
        log.info("Setting story points for {} issues", issuesFromJira.size());

        for (Issue issueToUpdate : issuesFromJira) {
            try {
                Number updatedPointsValue = issueToUpdate.fields.storyPoints;
                String pointsDisplayValue = updatedPointsValue != null ? String.valueOf(updatedPointsValue) : "no";
                Issue existingIssue = jira.getIssueByKey(issueToUpdate.key);
                if (existingIssue.fields.storyPointsEqual(updatedPointsValue)) {
                    log.info("Issue {} is already set at {} story points, no need to update"
                            , issueToUpdate.key, pointsDisplayValue);
                    continue;
                }
                jira.updateIssueStoryPointsOnly(issueToUpdate);
                log.debug("Updated story points to {} for issue {}", pointsDisplayValue, issueToUpdate.key);
            } catch (NotFoundException e) {
                // ignore if the issue does not exist anymore in JIRA
                log.info("Ignoring missing issue '{}'", issueToUpdate.key);
            }
        }
    }
}
