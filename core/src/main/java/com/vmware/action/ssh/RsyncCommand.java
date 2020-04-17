package com.vmware.action.ssh;

import java.io.File;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Executes a rsync command using the specified source directory and destination directory")
public class RsyncCommand extends BaseSingleVappJsonAction {
    public RsyncCommand(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("rsyncSourcePath", "rsyncDestinationPath");
    }

    @Override
    public void process() {
        SiteConfig siteConfigToUse = createSshSiteConfig();
        siteConfigToUse.validate();
        executeRsyncCommand(siteConfigToUse);
    }

    protected void executeRsyncCommand(SiteConfig siteConfig) {
        File sourcePath = FileUtils.determineFullPath(sshConfig.rsyncSourcePath);
        String rsyncDeleteFlag = sshConfig.rsyncDeleteRemovedFiles ? " --delete" : "";
        String rsyncCommand = String.format("rsync -%s %s %s@%s:%s%s",
                sshConfig.rsyncFlags, sourcePath.getPath(), siteConfig.username, siteConfig.host, sshConfig.rsyncDestinationPath, rsyncDeleteFlag);
        log.info("Executing rsync command {}", rsyncCommand);
        CommandLineUtils.executeCommand(rsyncCommand, LogLevel.INFO);
    }
}
