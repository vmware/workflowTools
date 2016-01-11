package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.http.exception.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Bulk update the story points for jira issues.")
public class UpdateStoryPointsOnly extends BaseBatchJiraAction {

    public UpdateStoryPointsOnly(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (multiActionData.getIssuesFromJira().isEmpty()) {
            return "there are no issues loaded from Jira";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        List<Issue> issuesFromJira = multiActionData.getIssuesFromJira();
        log.info("Setting story points for {} issues", issuesFromJira.size());

        for (Issue issueToUpdate : issuesFromJira) {
            try {
                Number updatedPointsValue = issueToUpdate.fields.storyPoints;
                String pointsDisplayValue = updatedPointsValue != null ? String.valueOf(updatedPointsValue) : "no";
                Issue existingIssue = jira.getIssueByKey(issueToUpdate.getKey());
                if (existingIssue.fields.storyPointsEqual(updatedPointsValue)) {
                    log.info("Issue {} is already set at {} story points, no need to update"
                            , issueToUpdate.getKey(), pointsDisplayValue);
                    continue;
                }
                jira.updateIssueStoryPointsOnly(issueToUpdate);
                log.debug("Updated story points to {} for issue {}", pointsDisplayValue, issueToUpdate.getKey());
            } catch (NotFoundException e) {
                // ignore if the issue does not exist anymore in JIRA
                log.info("Ignoring missing issue '{}'", issueToUpdate.getKey());
            }
        }
    }
}
