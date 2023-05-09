package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitCreateAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;

import java.io.File;

@ActionDescription("Runs git pre-commit hooks if the file exists and the skip pre-commits is fals")
public class RunPrecommitHooksIfNeeded extends BaseAction {
    public RunPrecommitHooksIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(!git.workingDirectoryIsInGitRepo(), "not using a git repository");
    }

    @Override
    public void process() {
        if (gitRepoConfig.noVerify) {
            log.debug("noVerify is set to true, not running pre-commit hook");
        }
        File rootDirectory = git.getRootDirectory();
        File precommitFile = new File(rootDirectory.getAbsolutePath()
                + File.separator + ".git" + File.separator + "hooks" + File.separator + "pre-commit");
        if (!precommitFile.exists()) {
            log.debug("Pre-commit file {} does not exist", precommitFile.getAbsolutePath());
            return;
        }
        if (!precommitFile.canExecute()) {
            throw new FatalException("Pre-commit file {} can not be executed", precommitFile.getAbsolutePath());
        }
        log.info("Running pre-commit file {} to check for issues", precommitFile);
        CommandLineUtils.executeCommand(precommitFile.getAbsolutePath(), LogLevel.INFO);
    }
}
