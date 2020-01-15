package com.vmware.action.gitlab;

import java.util.Arrays;
import java.util.Optional;

import com.vmware.action.base.BaseCommitUsingGitlabAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;

@ActionDescription("Selects the matching merger request in Gitlab by merge branch")
public class SelectMatchingMergeRequest extends BaseCommitUsingGitlabAction {
    public SelectMatchingMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String sourceMergeBranch = determineSourceMergeBranch();
        String targetMergeBranch = determineTargetMergeBranch();
        log.info("Checking merge requests for request matching source branch {} and target branch {}", sourceMergeBranch, targetMergeBranch);

        MergeRequest[] userMergeRequests = gitlab.getMergeRequests("opened");
        Optional<MergeRequest> matchingRequest = Arrays.stream(userMergeRequests).filter(request -> matches(request, sourceMergeBranch, targetMergeBranch)).findFirst();
        if (matchingRequest.isPresent()) {
            log.info("Found matching merge request {}", matchingRequest.get().webUrl);
            draft.setGitlabMergeRequest(matchingRequest.get());
        } else {
            log.info("Failed to find matching merge request");
        }

    }

    private boolean matches(MergeRequest mergeRequest, String sourceMergeBranch, String targetMergeBranch) {
        return mergeRequest.projectId == gitlabConfig.gitlabProjectId
                && mergeRequest.sourceBranch.equals(sourceMergeBranch) && mergeRequest.targetBranch.equals(targetMergeBranch);
    }
}
