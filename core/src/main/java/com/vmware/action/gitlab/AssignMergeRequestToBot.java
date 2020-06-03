package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitUsingGitlabAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;

@ActionDescription("For gitlab workflows that only allow a bot user to merge a merge request. This action changes the merge request assignee to the specified user id.")
public class AssignMergeRequestToBot extends BaseCommitUsingGitlabAction {
    public AssignMergeRequestToBot(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Assigning merge request {} to merge bot user with id {}", draft.mergeRequestUrl, gitlabConfig.mergeBotUserId);
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        mergeRequest.assigneeId = gitlabConfig.mergeBotUserId;
        gitlab.updateMergeRequest(mergeRequest);
    }
}
