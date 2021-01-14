package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.SystemUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Uses psql to create a new database from a backup file.")
public class CreatePostgresDatabaseFromFile extends BaseAction {
    public CreatePostgresDatabaseFromFile(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql", "createdb");
        super.addFailWorkflowIfBlankProperties("sourceFile", "databaseSchemaName", "databaseUsername");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (SystemUtils.postgresSchemaExists(fileSystemConfig.databaseSchemaName)) {
            skipActionDueTo("{} schema already exists", fileSystemConfig.databaseSchemaName);
        }
    }

    @Override
    public void process() {
        String sourceFilePath = fileSystemConfig.sourceFile;
        String databaseSchemaName = fileSystemConfig.databaseSchemaName;
        log.info("Creating postgres database {} from backup file {}", databaseSchemaName, sourceFilePath);
        CommandLineUtils.executeCommand("createdb -O " + fileSystemConfig.databaseUsername + " " + databaseSchemaName, LogLevel.INFO);


        String populateCommand = String.format("psql -U %s -d %s -f %s", fileSystemConfig.databaseUsername, databaseSchemaName, sourceFilePath);
        log.info("Executing command for populating database\n{}", populateCommand);
        CommandLineUtils.executeCommand(populateCommand, LogLevel.DEBUG);
    }
}
