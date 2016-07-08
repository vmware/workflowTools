package com.vmware.action.gitchangeset;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Adds the git config value changesetsync.checkoutdir to reference the client root directory if needed.")
public class AddGitChangesetConfigValueIfNeeded extends BasePerforceCommitAction {

    public AddGitChangesetConfigValueIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String existingCheckoutDir = git.configValue("changesetsync.checkoutdir");
        String expectedCheckoutDir = perforce.getWorkingDirectory().getPath();
        if (StringUtils.isBlank(existingCheckoutDir)) {
            log.info("Adding git config value changesetsync.checkoutdir={} for git changeset setup", expectedCheckoutDir);
            git.addConfigValue("changesetsync.checkoutdir", expectedCheckoutDir);
        } else if (!expectedCheckoutDir.equals(existingCheckoutDir)) {
            log.warn("Expected directory {} for client {} but git config value changesetsync.checkoutdir is {}",
                    expectedCheckoutDir, config.perforceClientName, existingCheckoutDir);
        } else {
            log.debug("git config value changesetsync.checkoutdir matches expected directory {}", existingCheckoutDir);
        }
    }
}
