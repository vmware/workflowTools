package com.vmware.action.perforce;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Primarily used for testing, moves to a new branch, applies the changelist, compares content to old branch.")
public class ApplyChangelistToSeparateGitBranch extends ApplyChangelistDiffToGitBranch {

    public ApplyChangelistToSeparateGitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no changelist associated with commit";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        String existingRef = git.revParse("head");
        String newBranchName = "changelist" + draft.perforceChangelistId;
        log.info("Moving to new branch {} tracking {}", newBranchName, gitRepoConfig.trackingBranchPath());
        git.newTrackingBranch(newBranchName, gitRepoConfig.trackingBranchPath());
        super.process();
        String diffData = git.diff(existingRef, true);
        if (StringUtils.isNotBlank(diffData)) {
            throw new RuntimeException("Diff must not have applied cleanly to branch " + newBranchName
                    + " , diff output\n" + diffData);
        } else {
            log.info("Diff applied cleanly to branch {}!", newBranchName);
            log.info("Creating commit for changes");
            git.commitWithAllFileChanges(draft.toText(commitConfig));
        }
    }
}
