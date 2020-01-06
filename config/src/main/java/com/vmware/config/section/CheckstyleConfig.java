package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

import java.util.List;

public class CheckstyleConfig {

    @ConfigurableProperty(help = "Path to jar for checkstyle")
    public String checkstyleJarPath;

    @ConfigurableProperty(commandLine = "--checkstyle-config-xml-path", help = "Path to config xml for checkstyle")
    public String checkstyleConfigXmlPath;

    @ConfigurableProperty(help = "Path to suppressions xml for checkstyle")
    public String checkstyleSuppressionsXmlPath;

    @ConfigurableProperty(help = "Run checkstyle on a file if it starts with a file mapping")
    public List<String> checkstyleFileMappings;
}
