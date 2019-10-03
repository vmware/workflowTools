package com.vmware.config.section;

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

    @ConfigurableProperty(commandLine = "--vcd-vm-quota", help = "User VM quota count for Vcloud Director")
    public int vcdVmQuota;

    @ConfigurableProperty(commandLine = "--wait-for-vapp-delete", help = "Wait for Vapp deletion task to complete")
    public boolean waitForDeleteTaskCompletion;

    @ConfigurableProperty(commandLine = "--vapp-lease", help = "Lease value in days for the selected Vapp")
    public int vappLeaseValue;

    @ConfigurableProperty(commandLine = "--vcd-site-index", help = "Vcd site index in testbed json to use for ssh commands. Default is 1.")
    public int vcdSiteIndex;

    @ConfigurableProperty(commandLine = "--vcd-cell-index", help = "Vcd cell index in testbed json to use for ssh commands. Default is 1.")
    public int vcdCellIndex;

    @ConfigurableProperty(help = "Directory to search for testbed templates")
    public String testbedTemplateDirectory;
}
