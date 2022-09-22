package com.vmware.action.base;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.domain.Sites;

@ActionDescription(value = "Base ssh action", configFlagsToExcludeFromCompleter = "--use-database-host")
public abstract class BaseSshAction extends BaseVappAction {

    public BaseSshAction(WorkflowConfig config) {
        super(config);
        JSch.setLogger(new SshLogger(log));
    }

    protected void waitForChannelToFinish(Channel channel) {
        while (channel.isConnected()) {
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }

    protected SiteConfig createSshSiteConfig() {
        if (vappData.getSelectedSite() != null && vcdConfig.useDatabaseHost) {
            Sites.Site site = vappData.getSelectedSite();
            Sites.DatabaseServer databaseConfig = site.databaseServer;
            log.info("Using database host {} for ssh site config", databaseConfig.host);
            return new SiteConfig(databaseConfig.host, 22, databaseConfig.getSshCredentials().username, databaseConfig.getSshCredentials().password);
        } else if (vappData.getSelectedVm() != null) {
            Sites.VmInfo vm = vappData.getSelectedVm();
            Sites.Credentials credentials = vm.getSshCredentials();
            if (credentials == null) {
                if (StringUtils.isEmpty(sshConfig.sshUsername) || StringUtils.isEmpty(sshConfig.sshPassword)) {
                    throw new FatalException("No ssh credentials found for VM {}, Please set with --ssh-username and --ssh-password", vm.getName());
                }
                credentials = new Sites().new Credentials(sshConfig.sshUsername, sshConfig.sshPassword);
            }
            return new SiteConfig(vm.getHost(), 22, credentials.username, credentials.password);
        } else if (sshConfig.hasCommandLineSite()) {
            return sshConfig.commandLineSite();
        } else {
            String sshSite = sshConfig.sshSite;
            TreeMap<String, SiteConfig> sshSiteConfigs = sshConfig.sshSiteConfigs;
            if (sshSiteConfigs == null || sshSiteConfigs.isEmpty()) {
                throw new FatalException("No ssh sites configured");
            }
            if (StringUtils.isEmpty(sshSite)) {
                sshSite = InputUtils.readValueUntilNotBlank("Ssh site", sshSiteConfigs.keySet());
            }
            if (!sshSiteConfigs.containsKey(sshSite)) {
                throw new FatalException("Ssh site {} is not present in list {}", sshSite, sshSiteConfigs.keySet().toString());
            }
            return sshSiteConfigs.get(sshSite);
        }
    }

    private static class SshLogger implements Logger {

        private DynamicLogger logger;

        SshLogger(org.slf4j.Logger logger) {
            this.logger = new DynamicLogger(logger);
        }

        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            logger.log(LogLevel.fromJschLevel(level), message);
        }
    }
}
