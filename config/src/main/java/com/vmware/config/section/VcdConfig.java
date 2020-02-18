package com.vmware.config.section;

import java.util.List;

import com.vmware.config.ConfigurableProperty;

public class VcdConfig {
    @ConfigurableProperty(commandLine = "--vcd-url", help = "Url for Vcloud Director")
    public String vcdUrl;

    @ConfigurableProperty(commandLine = "--default-vcd-org", help = "Default Org to use for user login if none specified")
    public String defaultVcdOrg;

    @ConfigurableProperty(commandLine = "--vcd-api-version", help = "Api version to use for Vcloud Director")
    public String vcdApiVersion;

    @ConfigurableProperty(commandLine = "--vcd-check-quota", help = "Whether to check if user is over VM quota count")
    public boolean checkVmQuota;

    @ConfigurableProperty(commandLine = "--vcd-vm-quota", help = "User VM quota count for Vcloud Director. Used if quota cannot be found in Vcloud Director")
    public int vcdVmQuota;

    @ConfigurableProperty(commandLine = "--vapp-lease", help = "Lease value in days for the selected Vapp")
    public int vappLeaseValue;

    @ConfigurableProperty(commandLine = "--vapp-name", help = "Specicy vapp name to use, mainly used for scripting")
    public String vappName;

    @ConfigurableProperty(commandLine = "--vcd-site-index", help = "Vcd site index in testbed json to use for ssh commands. Default is 1.")
    public Integer vcdSiteIndex;

    @ConfigurableProperty(commandLine = "--vcd-cell-index", help = "Vcd cell index in testbed json to use for ssh commands. Default is 1.")
    public Integer vcdCellIndex;

    @ConfigurableProperty(help = "Directory to search for testbed templates")
    public String testbedTemplateDirectory;

    @ConfigurableProperty(commandLine = "--vcd-tenant", help = "Name of tenant to open UI page for")
    public String vcdTenant;

    @ConfigurableProperty(help = "Preset list of vapp json files")
    public List<String> vappJsonFiles;

    @ConfigurableProperty(commandLine = "--vapp-json-file", help = "Name of file to load json from")
    public String vappJsonFile;
}
