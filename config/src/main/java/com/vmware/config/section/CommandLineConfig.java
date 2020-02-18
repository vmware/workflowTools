package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

/**
 * Configuration for command line integration
 */
public class CommandLineConfig {
    @ConfigurableProperty(commandLine = "--workflow-alias", help = "Alias used on command line to reference workflow tools")
    public String workflowAlias;

    @ConfigurableProperty(commandLine = "--include-alias-autocomplete", help = "Whether to include auto complete support for workflow aliases")
    public boolean includeAliasAutocomplete;

    @ConfigurableProperty(commandLine = "--autocomplete-workflow", help = "Auto complete values for specified workflow")
    public String autocompleteWorkflow;
}
