package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class BuildwebConfig {

    @ConfigurableProperty(commandLine = "-buildwebUrl,--buildweb-url", help = "Url for buildweb server")
    public String buildwebUrl;

    @ConfigurableProperty(commandLine = "-gobuildBinPath,--gobuild-bin-path", help = "Path to gobuild bin file, this is a VMware specific tool")
    public String goBuildBinPath;

    @ConfigurableProperty(commandLine = "-buildwebProject,--buildweb-project", help = "Which buildweb project to use for a gobuild sandbox buikd, this is for a VMware specific tool")
    public String buildwebProject;

    @ConfigurableProperty(commandLine = "-buildwebBranch,--buildweb-branch", help = "Which branch on buildweb to use for a gobuild sandbox build, this is for a VMware specific tool")
    public String buildwebBranch;

    @ConfigurableProperty(commandLine = "-buildType,--build-type", help = "Buildweb build type to use, this is for a VMware specific tool")
    public String buildType;

    @ConfigurableProperty(commandLine = "-buildwebApiUrl,--buildweb-api-url", help = "Api Url for buildweb server")
    public String buildwebApiUrl;

    @ConfigurableProperty(commandLine = "--buildweb-logs-url-pattern", help = "Url patter for buildweb logs")
    public String buildwebLogsUrlPattern;

    @ConfigurableProperty(commandLine = "-syncToBranchLatest,--sync-to-branch-latest", help = "By default, files to be synced to the latest in perforce, this flag syncs them to the latest changelist known to the git branch")
    public boolean syncChangelistToLatestInBranch;
}
