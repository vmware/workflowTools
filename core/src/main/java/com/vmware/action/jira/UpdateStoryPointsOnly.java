package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.http.exception.NotFoundException;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Bulk update the story points for jira issues.")
public class UpdateStoryPointsOnly extends BaseBatchJiraAction {

    public UpdateStoryPointsOnly(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(projectIssues.getIssuesFromJira().isEmpty(), "there are no issues loaded from Jira");
    }

    @Override
    public void process() {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
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
                if (StringUtils.isNotEmpty(projectIssues.boardId)) {
                    jira.updateIssueStoryPointsUsingAgileApi(issueToUpdate, projectIssues.boardId);
                } else {
                    jira.updateIssueStoryPointsOnly(issueToUpdate);
                }
                log.info("Updated story points to {} for issue {}", pointsDisplayValue, issueToUpdate.getKey());
            } catch (NotFoundException e) {
                // ignore if the issue does not exist anymore in JIRA
                log.info("Ignoring missing issue '{}'", issueToUpdate.getKey());
            }
        }
    }
}
