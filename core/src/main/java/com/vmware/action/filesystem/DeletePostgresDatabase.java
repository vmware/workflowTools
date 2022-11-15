package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.SystemUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Uses psql to delete a database.")
public class DeletePostgresDatabase extends BaseAction {
    public DeletePostgresDatabase(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql", "createdb");
        super.addFailWorkflowIfBlankProperties("databaseSchemaName");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (!SystemUtils.postgresSchemaExists(fileSystemConfig.databaseSchemaName)) {
            skipActionDueTo("{} schema does not exist", fileSystemConfig.databaseSchemaName);
        }
    }

    @Override
    public void process() {
        String databaseSchemaName = fileSystemConfig.databaseSchemaName;
        log.info("Deleting postgres database {}", databaseSchemaName);
        CommandLineUtils.executeCommand("dropdb " + databaseSchemaName, LogLevel.INFO);
    }
}
