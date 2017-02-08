package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.StringUtils;

@ActionDescription("This MUST be used first to parse the last commit if intending to edit anything in the last commit.")
public class ReadLastCommit extends BaseCommitAction {

    public ReadLastCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        String reasonForFailing = gitRepoOrPerforceClientCanBeUsed();
        if (StringUtils.isNotBlank(reasonForFailing)) {
            return reasonForFailing;
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        String commitText = readLastChange();
        if (git.workingDirectoryIsInGitRepo()) {
            draft.branch = determineBranchName();
        }
        draft.fillValuesFromCommitText(commitText, config.getCommitConfiguration());
        if (git.workingDirectoryIsInGitRepo()) {
            log.info("Read last commit from branch {}", draft.branch);
        } else {
            log.info("Read pending changelist {}", draft.perforceChangelistId);
        }

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toText(config.getCommitConfiguration()));
        titlePadder.debugTitle();
    }

    private String determineBranchName() {
        String targetBranch = git.currentBranch();
        log.debug("Using local git branch {}", targetBranch);
        if (StringUtils.isNotBlank(config.targetBranch)) {
            log.info("Setting branch property to {} (read from application config)", targetBranch);
            targetBranch = config.targetBranch;
        }
        return targetBranch;
    }
}
