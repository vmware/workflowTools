package com.vmware.config.section;

import java.util.stream.Stream;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

public class FileSystemConfig {
    @ConfigurableProperty(commandLine = "--source-url", help = "Source url to copy certificates from")
    public String sourceUrl;

    @ConfigurableProperty(commandLine = "--source-file", help = "Source file to use")
    public String sourceFile;

    @ConfigurableProperty(commandLine = "--destination-file", help = "Destination path to save file to")
    public String destinationFile;

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

    @ConfigurableProperty(commandLine = "--database-schema-name", help = "Database schema name to copy database as")
    public String databaseSchemaName;

    @ConfigurableProperty(commandLine = "--input-text", help = "Text to use for searching or appending to file")
    public String inputText;

    @ConfigurableProperty(commandLine = "--replacement-text", help = "Replacement text for matches")
    public String replacementText;

    @ConfigurableProperty(commandLine = "--json-property-path", help = "Json property path")
    public String jsonPropertyPath;

    public boolean databaseConfigured() {
        return Stream.of(databaseUrl, databaseUsername, databasePassword).allMatch(StringUtils::isNotBlank);
    }
}
