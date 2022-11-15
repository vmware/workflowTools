package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;

@ActionDescription(value = "Execute a sql statement locally using psql.", configFlagsToExcludeFromCompleter = "--output-variable-name")
public class ExecuteLocalSqlStatementUsingPsql extends BaseAction {
    public ExecuteLocalSqlStatementUsingPsql(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("psql");
        super.addFailWorkflowIfBlankProperties("databaseUsername", "databaseSchemaName", "sqlStatement");
    }

    @Override
    public void process() {
        log.info("Executing sql statement {} using schema {}", fileSystemConfig.sqlStatement, fileSystemConfig.databaseSchemaName);
        String command = String.format("psql -t -U %s -d %s -c \"%s\"", fileSystemConfig.databaseUsername,
                fileSystemConfig.databaseSchemaName, fileSystemConfig.sqlStatement);
        String output = StringUtils.trim(CommandLineUtils.executeCommand(command, LogLevel.INFO));

        if (StringUtils.textStartsWithValue(output, "psql: error")) {
            throw new FatalException(output);
        }

        String missingTable = MatcherUtils.singleMatch(output, "relation \"([\\w_]+)\" does not exist");
        if (StringUtils.isNotBlank(missingTable) && StringUtils.isNotBlank(fileSystemConfig.outputVariableName)) {
            log.info("Skipping setting value for output variable {} as table {} does not exist", fileSystemConfig.outputVariableName, missingTable);
        } else if (StringUtils.isNotBlank(missingTable)) {
            throw new FatalException(output);
        } else if (StringUtils.isNotBlank(output) && StringUtils.isNotBlank(fileSystemConfig.outputVariableName)) {
            log.info("Saving output {} to output variable {}", output, fileSystemConfig.outputVariableName);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, output);
        } else if (StringUtils.isNotBlank(fileSystemConfig.outputVariableName)) {
            log.info("No value to set to output variable {}", fileSystemConfig.outputVariableName);
        }
    }
}
