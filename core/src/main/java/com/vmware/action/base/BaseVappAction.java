package com.vmware.action.base;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.domain.Sites;
import com.vmware.vcd.domain.VappData;

public abstract class BaseVappAction extends BaseCommitAction {
    protected boolean checkVappJson;
    protected boolean checkIfSiteSelected;
    protected boolean checkIfCellSelected;
    protected VappData vappData;

    public BaseVappAction(WorkflowConfig config) {
        super(config);
        JSch.setLogger(new SshLogger(log));
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (checkVappJson && StringUtils.isBlank(draft.vappJsonForJenkinsJob)) {
            return "no Vapp json loaded";
        }
        if ((checkIfSiteSelected || checkIfSiteSelected) && vappData.getSelectedSite() == null) {
            return "no vcd site selected";
        }
        if (checkIfCellSelected && vappData.getSelectedVcdCell() == null) {
            return "no vcd cell selected";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    public void setVappData(VappData vappData) {
        this.vappData = vappData;
    }

    protected void waitForChannelToFinish(Channel channel) {
        while (channel.isConnected()) {
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }

    protected SiteConfig createSshSiteConfig() {
        if (vappData.getSelectedVcdCell() != null) {
            Sites.DeployedVM cell = vappData.getSelectedVcdCell();
            Sites.OvfProperties ovfProperties = cell.deployment.ovfProperties;
            return new SiteConfig(ovfProperties.hostname, 22, cell.osCredentials.username, cell.osCredentials.password);
        } else if (sshConfig.useSshSite()) {
            String sshSite = sshConfig.sshSite;
            TreeMap<String, SiteConfig> sshSiteConfigs = sshConfig.sshSiteConfigs;
            if (StringUtils.isBlank(sshSite)) {
                sshSite = InputUtils.readValueUntilNotBlank("Ssh site", sshSiteConfigs.keySet());
            }
            if (!sshSiteConfigs.containsKey(sshSite)) {
                throw new FatalException("Ssh site {} is not present in list {}", sshSite, sshSiteConfigs.keySet().toString());
            }
            return sshSiteConfigs.get(sshSite);
        } else {
            return sshConfig.commandLineSite();
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
