package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.SystemUtils;
import com.vmware.util.exception.CancelException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Exit if existing postgres database should not be reused.")
public class ExitIfReusingPostgresDatabaseNotAllowed extends BaseAction {
    public ExitIfReusingPostgresDatabaseNotAllowed(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql");
    }

    @Override
    public void checkIfWorkflowShouldBeFailed() {
        super.checkIfWorkflowShouldBeFailed();
        super.failIfTrue(StringUtils.isEmpty(fileSystemConfig.databaseSchemaName), "No database schema name set");
    }

    @Override
    public void process() {
        String databaseSchemaName = fileSystemConfig.databaseSchemaName;
        if (SystemUtils.postgresSchemaExists(databaseSchemaName)) {
            String confirmation = InputUtils.readValue("Postgres database " + databaseSchemaName + " already exists, reuse database? (yes/no)");
            if (!confirmation.equalsIgnoreCase("yes")) {
                log.info("run command dropdb {} to drop database if needed", databaseSchemaName);
                throw new CancelException(LogLevel.INFO, "database already exists");
            }
        }

    }
}
