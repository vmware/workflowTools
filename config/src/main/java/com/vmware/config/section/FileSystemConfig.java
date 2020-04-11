package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class FileSystemConfig {
    @ConfigurableProperty(commandLine = "--source-file", help = "Source file to copy")
    public String sourceFile;

    @ConfigurableProperty(commandLine = "--destination-file", help = "Destination path to copy file to")
    public String destinationFile;

    @ConfigurableProperty(commandLine = "--property-file", help = "Property file to use")
    public String propertyFile;

    @ConfigurableProperty(commandLine = "--property-name", help = "PropertyName")
    public String propertyName;

    @ConfigurableProperty(commandLine = "--property-value", help = "Property value")
    public String propertyValue;

    @ConfigurableProperty(commandLine = "--backup-name", help = "Name to append to file / directory being backed up")
    public String backupName;

    @ConfigurableProperty(commandLine = "--skip-backup", help = "Skip backup action")
    public boolean skipBackup;

    @ConfigurableProperty(commandLine = "--sql-statement", help = "Sql statement to execute")
    public String sqlStatement;

    @ConfigurableProperty(commandLine = "--database-driver-file", help = "Database driver file path")
    public String databaseDriverFile;

    @ConfigurableProperty(commandLine = "--database-driver-class", help = "Database driver class to use")
    public String databaseDriverClass;

    @ConfigurableProperty(commandLine = "--database-url-pattern", help = "Database url pattern to use if using a Vapp")
    public String databaseUrlPattern;

    @ConfigurableProperty(commandLine = "--database-url", help = "Database url")
    public String databaseUrl;

    @ConfigurableProperty(commandLine = "--database-username", help = "Database username")
    public String databaseUsername;

    @ConfigurableProperty(commandLine = "--database-password", help = "Database user password")
    public String databasePassword;
}
