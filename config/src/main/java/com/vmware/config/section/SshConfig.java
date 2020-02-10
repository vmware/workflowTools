package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;

import java.util.TreeMap;

import static com.vmware.util.StringUtils.isEmpty;
import static com.vmware.util.StringUtils.isNotEmpty;

public class SshConfig {

    @ConfigurableProperty(help = "Ssh site configuration")
    public TreeMap<String, SiteConfig> sshSiteConfigs;

    @ConfigurableProperty(commandLine = "--ssh-site", help = "Ssh site to use. Set either site or all ssh values")
    public String sshSite;

    @ConfigurableProperty(commandLine = "--ssh-host", help = "Ssh host to use")
    public String sshHost;

    @ConfigurableProperty(commandLine = "--ssh-port", help = "Ssh port to use")
    public int sshPort;

    @ConfigurableProperty(commandLine = "--ssh-username", help = "Ssh username to use")
    public String sshUsername;

    @ConfigurableProperty(commandLine = "--ssh-password", help = "Ssh password to use")
    public String sshPassword;

    @ConfigurableProperty(commandLine = "--ssh-command", help = "Ssh command to execute")
    public String sshCommand;

    @ConfigurableProperty(commandLine = "--ssh-strict-host-checking", help = "Whether to enforce strict host checking for ssh")
    public boolean sshStrictHostChecking;

    @ConfigurableProperty(commandLine = "--log-file", help = "Log file(s) to tail or find matching lines from")
    public String logFile;

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--line-count-before", help = "How many lines of the log to show before a match")
    public int lineCountBeforeMatch;

    @ConfigurableProperty(commandLine = "--line-count-after", help = "How many lines of the log to show after a match")
    public int lineCountAfterMatch;

    @ConfigurableProperty(commandLine = "--tail-follow", help = "Using tail -f")
    public boolean continuousTailing;

    @ConfigurableProperty(commandLine = "--search-text", help = "Text to match using sed")
    public String searchText;

    @ConfigurableProperty(commandLine = "--rsync-flags", help = "Flags to use for rsync command")
    public String rsyncFlags;

    @ConfigurableProperty(commandLine = "--rsync-source", help = "Source path for to use for rsync command")
    public String rsyncSourcePath;

    @ConfigurableProperty(commandLine = "--rsync-destination", help = "Destination path for to use for rsync command")
    public String rsyncDestinationPath;

    @ConfigurableProperty(commandLine = "--rsync-delete", help = "Delete files not in the source path from the destination")
    public boolean rsyncDeleteRemovedFiles;

    public SiteConfig commandLineSite() {
        return new SiteConfig(sshHost, sshPort, sshUsername, sshPassword);
    }

    public boolean usesSshSite() {
        return StringUtils.isNotEmpty(sshSite) || hasCommandLineSite();
    }

    public boolean hasCommandLineSite() {
        return isNotEmpty(sshHost) && isNotEmpty(sshUsername) && isNotEmpty(sshPassword);
    }
}
