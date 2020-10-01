package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Exit if postgres database already exists.")
public class ExitIfPostgresDatabaseAlreadyExists extends BaseAction {
    public ExitIfPostgresDatabaseAlreadyExists(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql");
        super.addFailWorkflowIfBlankProperties("databaseSchemaName");
    }

    @Override
    public void process() {
        String sqlCommand = "select not exists(SELECT datname FROM pg_catalog.pg_database WHERE lower(datname) = '"
                + fileSystemConfig.databaseSchemaName + "')";
        String command = String.format("psql -t -c \"%s\"", sqlCommand);
        String output = CommandLineUtils.executeCommand(command, LogLevel.DEBUG);
        if (!"t".equals(StringUtils.trim(output))) {
            cancelWithMessage("Postgres database " + fileSystemConfig.databaseSchemaName + " already exists, "
                    + "run command dropdb " + fileSystemConfig.databaseSchemaName + " to drop database");
        }

    }
}
