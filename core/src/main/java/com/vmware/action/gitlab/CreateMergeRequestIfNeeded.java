package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitUsingGitlabAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;

@ActionDescription("Creates a merge request in gitlab, uses merge request branch format unless one specified.")
public class CreateMergeRequestIfNeeded extends BaseCommitUsingGitlabAction {
    public CreateMergeRequestIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.hasMergeOrPullRequest(), "merge request with url " + draft.requestUrl + " has already been created");
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.title = gitRepoConfig.markAsDraft ? gitlabConfig.draftMergeRequestPrefix + " " + draft.summary : draft.summary;
        mergeRequest.targetProjectId = gitlabConfig.gitlabProjectId;
        mergeRequest.sourceBranch = determineSourceMergeBranch();
        mergeRequest.targetBranch = determineTargetMergeBranch();
        mergeRequest.removeSourceBranch = true;
        mergeRequest.squash = true;

        log.info("Creating merge request with source branch {} and target branch {}", mergeRequest.sourceBranch, mergeRequest.targetBranch);
        MergeRequest createdRequest = gitlab.createMergeRequest(mergeRequest);
        draft.setGitlabMergeRequest(createdRequest);
        log.info("Created merge request {}", createdRequest.webUrl);
    }
}
