package com.vmware.action.gitchangeset;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.io.File;
import java.io.IOException;

@ActionDescription("Adds the git config value changesetsync.checkoutdir to reference the client root directory if needed.")
public class AddGitChangesetConfigValueIfNeeded extends BasePerforceCommitAction {

    public AddGitChangesetConfigValueIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String clientDirectory = perforce.getWorkingDirectory().getPath();
        String changesetCheckoutPath = determineChangesetDirectoryCanonicalPath();

        if (StringUtils.isBlank(changesetCheckoutPath)) {
            log.info("Adding git config value changesetsync.checkoutdir={} for git changeset setup", clientDirectory);
            git.addConfigValue("changesetsync.checkoutdir", clientDirectory);
        } else if (!clientDirectory.equals(changesetCheckoutPath)) {
            log.warn("Expected directory {} for client {} to match git config value changesetsync.checkoutdir {}",
                    clientDirectory, config.perforceClientName, changesetCheckoutPath);
        } else {
            log.debug("git config value changesetsync.checkoutdir matches expected directory {}", changesetCheckoutPath);
        }
    }

    private String determineChangesetDirectoryCanonicalPath() {
        String checkoutDirectory = git.configValue("changesetsync.checkoutdir");
        if (StringUtils.isNotBlank(checkoutDirectory)) {
            try {
                checkoutDirectory = new File(checkoutDirectory).getCanonicalPath();
            } catch (IOException e) {
                log.warn("Cannot get canonical path for changesetsync.checkoutdir value {}: {}", checkoutDirectory,
                        e.getMessage());
            }
        }
        return checkoutDirectory;
    }
}
