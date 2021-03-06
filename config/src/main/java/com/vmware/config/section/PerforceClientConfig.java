package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class PerforceClientConfig {

    @ConfigurableProperty(gitConfigProperty = "git-p4.client", help = "Perforce client to use")
    public String perforceClientName;

    @ConfigurableProperty(gitConfigProperty = "changesetsync.checkoutdir", help = "Perforce client root directory can be explicitly specified if desired")
    public String perforceClientDirectory;

    @ConfigurableProperty(commandLine = "-cId,--changelist-id", help = "ID of perforce changelist to use")
    public String changelistId;
}
