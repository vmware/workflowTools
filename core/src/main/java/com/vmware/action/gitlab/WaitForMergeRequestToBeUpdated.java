package com.vmware.action.gitlab;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.util.ThreadUtils;

@ActionDescription("Wait for gitlab merge request commit hash to match the commit hash of the current branch head.")
public class WaitForMergeRequestToBeUpdated extends BaseCommitWithMergeRequestAction {

    public WaitForMergeRequestToBeUpdated(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        String headRef = git.revParse("HEAD");
        String currentBranch = git.currentBranch();
        if (headRef.equals(draft.getGitlabMergeRequest().sha)) {
            log.debug("Merge request {} commit hash already matches branch {} ref {}", draft.mergeRequestId(), currentBranch, headRef);
            return;
        }
        log.info("Waiting for merge request {} commit hash to be updated to match branch {} ref {}", draft.mergeRequestId(), currentBranch, headRef);
        Callable<Boolean> commitHashCheck = () -> {
            MergeRequest mergeRequest = gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId());
            draft.setGitlabMergeRequest(mergeRequest);
            log.info("Current merge request commit hash " + mergeRequest.sha);
            return headRef.equals(mergeRequest.sha);
        };

        ThreadUtils.waitForCallable(commitHashCheck, config.waitTimeForBlockingWorkflowAction, 3,
                "Merge request failed to be updated with sha " + headRef);
    }
}
