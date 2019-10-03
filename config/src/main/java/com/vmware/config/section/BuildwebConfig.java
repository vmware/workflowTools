package com.vmware.config.section;

import java.util.Map;

import com.vmware.config.ConfigurableProperty;

public class BuildwebConfig {

    @ConfigurableProperty(commandLine = "--buildweb-url", help = "Url for buildweb server")
    public String buildwebUrl;

    @ConfigurableProperty(commandLine = "--gobuild-bin-path", help = "Path to gobuild bin file, this is a VMware specific tool")
    public String goBuildBinPath;

    @ConfigurableProperty(commandLine = "-buildwebProject,--buildweb-project", help = "Which buildweb project to use for a gobuild sandbox buikd, this is for a VMware specific tool")
    public String buildwebProject;

    @ConfigurableProperty(commandLine = "--buildweb-branch", help = "Which branch on buildweb to use for a gobuild sandbox build, this is for a VMware specific tool")
    public String buildwebBranch;

    @ConfigurableProperty(commandLine = "--build-type", help = "Buildweb build type to use, this is for a VMware specific tool")
    public String buildType;

    @ConfigurableProperty(commandLine = "--component-builds", help = "Component builds value, this is for a VMware specific tool")
    public String componentBuilds;

    @ConfigurableProperty(commandLine = "--store-trees", help = "Adds --store-trees to the gobuild command, this is for a VMware specific tool")
    public boolean storeTrees;

    @ConfigurableProperty(commandLine = "--buildweb-api-url", help = "Api Url for buildweb server")
    public String buildwebApiUrl;

    @ConfigurableProperty(commandLine = "--buildweb-log-file-name", help = "Name of log file for buildweb build")
    public String buildwebLogFileName;

    @ConfigurableProperty(commandLine = "--sync-to-branch-latest", help = "By default, files to be synced to the latest in perforce, this flag syncs them to the latest changelist known to the git branch")
    public boolean syncChangelistToLatestInBranch;

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--include-in-progress", help = "Display output for in progress builds")
    public boolean includeInProgressBuilds;

    @ConfigurableProperty(commandLine = "--build-display-name", help = "Display name to use for the build invoked")
    public String buildDisplayName;

    @ConfigurableProperty(commandLine = "--exclude-sync-to", help = "Exclude sync-to parameter in gobuild command")
    public boolean excludeSyncTo;

    @ConfigurableProperty(help = "Git tracking branch mappings to buildweb branches")
    public Map<String, String> gitTrackingBranchMappings;
}
