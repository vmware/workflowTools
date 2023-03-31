package com.vmware.config.section;

import java.util.Properties;
import java.util.stream.Stream;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.StringUtils;

public class FileSystemConfig {

    @ConfigurableProperty(help = "Path to chrome executable")
    public String chromePath;

    @ConfigurableProperty(help = "Port to use for chrome remote debugging")
    public int chromeDebugPort;
    
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

    @ConfigurableProperty(commandLine = "--skip-backup", help = "Skip backup")
    public boolean skipBackup;

    @ConfigurableProperty(commandLine = "--backup-file-pattern", help = "Regex pattern to build list of backup files")
    public String backupFilePattern;

    @ConfigurableProperty(commandLine = "--overwrite-backup", help = "Overwrite existing file if present on backup")
    public boolean overwriteBackup;

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

    @ConfigurableProperty(commandLine = "--input-text", help = "Text to use")
    public String inputText;

    @ConfigurableProperty(commandLine = "--replacement-text", help = "Replacement text for matches")
    public String replacementText;

    @ConfigurableProperty(commandLine = "--json-property-path", help = "Json property path")
    public String jsonPropertyPath;

    @ConfigurableProperty(commandLine = "--file-data", help = "Loaded file data")
    public String fileData;

    @ConfigurableProperty(commandLine = "--regex", help = "Regular expression to use")
    public String regex;

    @ConfigurableProperty(commandLine = "--output-variable-name", help = "Name of variable used for output")
    public String outputVariableName;

    @ConfigurableProperty(commandLine = "--variable", help = "Name of variable to use")
    public String variable;

    @ConfigurableProperty(commandLine = "--skip-if-variable-set", help = "Skips action if the specified variable has a value")
    public boolean skipIfVariableSet;

    @ConfigurableProperty(commandLine = "--skip-if-variable-not-set", help = "Skips action if the specified variable does not have a value")
    public boolean skipIfVariableNotSet;

    @ConfigurableProperty(commandLine = "--auto-login", help = "Opens in Chrome and logs in using the credentials")
    public boolean autoLogin;

    @ConfigurableProperty(commandLine = "--repeat--count", help = "Repeating count")
    public int repeatCount;

    @ConfigurableProperty(commandLine = "--http-server-port", help = "Port for http server")
    public int httpServerPort;

    @ConfigurableProperty(commandLine = "--http-server-status-code", help = "Http Status code to return for http server")
    public int httpServerStatusCode;

    public boolean databaseConfigured() {
        return Stream.of(databaseUrl, databaseUsername, databasePassword).allMatch(StringUtils::isNotBlank);
    }

    public Properties dbConnectionProperties() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("url", databaseUrl);
        connectionProperties.put("user", databaseUsername);
        connectionProperties.put("password", databasePassword);
        return connectionProperties;
    }
}
