package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("This MUST be used first to parse the last commit if intending to edit anything in the last commit.")
public class ReadLastCommit extends BaseCommitAction {

    public ReadLastCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        draft.branch = determineBranchName();
        draft.fillValuesFromCommitText(git.lastCommitText(true), config.getCommitConfiguration());

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toGitText(config.getCommitConfiguration()));
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
