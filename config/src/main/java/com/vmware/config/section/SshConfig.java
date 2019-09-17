package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;

import java.util.TreeMap;

import static com.vmware.util.StringUtils.isBlank;

public class SshConfig {

    @ConfigurableProperty(help = "Ssh site configuration")
    public TreeMap<String, SiteConfig> sshSiteConfigs;

    @ConfigurableProperty(commandLine = "--ssh-site", help = "Ssh site to use. Set either site or all ssh values")
    public String sshSite;

    @ConfigurableProperty(commandLine = "--ssh-host", help = "Ssh host to use")
    public String sshHost;

    @ConfigurableProperty(commandLine = "--ssh-port", help = "Ssh port to use, no need to set if using port 22")
    public int sshPort;

    @ConfigurableProperty(commandLine = "--ssh-username", help = "Ssh username to use")
    public String sshUsername;

    @ConfigurableProperty(commandLine = "--ssh-password", help = "Ssh password to use")
    public String sshPassword;

    @ConfigurableProperty(commandLine = "--ssh-command", help = "Ssh command to execute")
    public String sshCommand;

    @ConfigurableProperty(commandLine = "--log-file", help = "Log file to tail")
    public String logFile;

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--tail-follow", help = "Using tail -f")
    public boolean continuousTailing;

    public SiteConfig commandLineSite() {
        return new SiteConfig(sshHost, sshPort, sshUsername, sshPassword);
    }

    public boolean useSshSite() {
        if (StringUtils.isNotBlank(sshSite)) {
            return true;
        }
        boolean sitesExist = sshSiteConfigs != null && !sshSiteConfigs.isEmpty();
        return sitesExist && noCommandLineSiteValues();
    }

    private boolean noCommandLineSiteValues() {
        return isBlank(sshHost) && isBlank(sshUsername) && isBlank(sshPassword);
    }
}
