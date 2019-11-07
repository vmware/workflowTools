package com.vmware.action.ssh;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Uses ssh-copy-id to copy ssh key to the remote site for passwordless login.")
public class CopySshIdToRemoteSite extends BaseVappAction {
    public CopySshIdToRemoteSite(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SiteConfig siteConfig = createSshSiteConfig();
        String copyIdCommand = String.format("ssh-copy-id %s@%s", siteConfig.username, siteConfig.host);
        log.info("Executing command {}", copyIdCommand);
        CommandLineUtils.executeCommand(copyIdCommand, LogLevel.INFO);
    }
}
