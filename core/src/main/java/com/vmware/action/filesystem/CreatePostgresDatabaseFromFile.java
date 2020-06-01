package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Uses psql to create a new database from a backup file.")
public class CreatePostgresDatabaseFromFile extends BaseAction {
    public CreatePostgresDatabaseFromFile(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("psql", "createdb");
        super.addFailWorkflowIfBlankProperties("sourceFile", "databaseSchemaName", "databaseUsername");
    }

    @Override
    public void process() {
        String sourceFilePath = fileSystemConfig.sourceFile;
        log.info("Creating postgres database {} from backup file {}", fileSystemConfig.databaseSchemaName, sourceFilePath);
        CommandLineUtils.executeCommand("createdb -O " + fileSystemConfig.databaseUsername + " " + fileSystemConfig.databaseSchemaName, LogLevel.INFO);


        String populateCommand = String.format("psql -U %s -d %s -f %s", fileSystemConfig.databaseUsername,
                fileSystemConfig.databaseSchemaName, sourceFilePath);
        log.info("Executing command for populating database\n{}", populateCommand);
        CommandLineUtils.executeCommand(populateCommand, LogLevel.DEBUG);
    }
}
