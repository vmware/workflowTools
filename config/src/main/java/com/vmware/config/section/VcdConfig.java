package com.vmware.config.section;

import java.util.Arrays;
import java.util.List;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

public class VcdConfig {
    @ConfigurableProperty(help = "Url for Vcloud Director")
    public String vcdUrl;

    @ConfigurableProperty(help = "Default Org to use for user login if none specified")
    public String defaultVcdOrg;

    @ConfigurableProperty(commandLine = "--vcd-sys-admin-username", help = "Username for the vcd sys admin user")
    public String vcdSysAdminUser;

    @ConfigurableProperty(commandLine = "--vcd-sys-admin-password", help = "Password for the vcd sys admin user")
    public String vcdSysAdminPassword;

    @ConfigurableProperty(help = "Api version to use for Vcloud Director")
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

    @ConfigurableProperty(commandLine = "--vapp-metadata-name", help = "Name of metadata property")
    public String vappMetadataName;

    @ConfigurableProperty(commandLine = "--use-database-host", help = "Use database host config for ssh site")
    public boolean useDatabaseHost;

    @ConfigurableProperty(commandLine = "--use-owned-vapps-only", help = "Only use a Vapp owned by the user, no file based Vapps")
    public boolean useOwnedVappsOnly;

    @ConfigurableProperty(commandLine = "--query-filter", help = "Optional filter to use when querying Vapps or VMs")
    public String queryFilter;

    @ConfigurableProperty(help = "Pattern for parsing build number for Vapp / Vm name")
    public String buildNumberInNamePattern;

    public String[] queryFilters() {
        if (StringUtils.isEmpty(queryFilter)) {
            return new String[0];
        }
        return new String[] {queryFilter};
    }
}
