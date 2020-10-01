package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Execute a sql statement locally using psql.")
public class ExecuteLocalSqlStatementUsingPsql extends BaseAction {
    public ExecuteLocalSqlStatementUsingPsql(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql");
        super.addFailWorkflowIfBlankProperties("databaseUsername", "databaseSchemaName", "sqlStatement");
    }

    @Override
    public void process() {
        log.info("Executing sql statement {} using schema {}", fileSystemConfig.sqlStatement, fileSystemConfig.databaseSchemaName);
        String command = String.format("psql -U %s -d %s -c \"%s\"", fileSystemConfig.databaseUsername,
                fileSystemConfig.databaseSchemaName, fileSystemConfig.sqlStatement);
        CommandLineUtils.executeCommand(command, LogLevel.INFO);

    }
}
