package com.vmware.action.base;

import java.util.TreeMap;

import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.VappData;

public abstract class BaseVappAction extends BaseCommitAction {
    protected VappData vappData;

    public BaseVappAction(WorkflowConfig config) {
        super(config);
    }

    public void setVappData(VappData vappData) {
        this.vappData = vappData;
    }

    protected SiteConfig createSshSiteConfig() {
        if (sshConfig.useSshSite()) {
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
}
