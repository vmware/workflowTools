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
        super(config);
    }

    @Override
    public void process() {
        String headRef = git.revParse("HEAD");
        log.info("Waiting for merge request {} commit to be updated to {}", draft.mergeRequestId(), headRef);
        Callable<Boolean> commitHashCheck = () -> {
            MergeRequest mergeRequest = gitlab.getMergeRequest(draft.mergeRequestProjectId(), draft.mergeRequestId());
            draft.setGitlabMergeRequest(mergeRequest);
            log.info("Current merge request commit hash " + mergeRequest.sha);
            if (!headRef.equals(mergeRequest.sha)) {
                return false;
            }
            return true;
        };

        ThreadUtils.waitForCallable(commitHashCheck, 30, TimeUnit.SECONDS, "Merge request failed to be updated with sha " + headRef);
    }
}
