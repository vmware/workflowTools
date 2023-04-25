package com.vmware.action.filesystem;

import com.google.gson.Gson;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.util.db.DbUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.domain.Sites;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

@ActionDescription("Executes the specified sql statement against the configured database. Assumes that it is an update command.")
public class ExecuteSelectSqlStatementAsJson extends ExecuteSqlStatement {

    public ExecuteSelectSqlStatementAsJson(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("databaseDriverClass", "databaseDriverFile", "sqlStatement");
    }

    @Override
    public void process() {
        String databaseUrl = fileSystemConfig.databaseConfigured() ? fileSystemConfig.databaseUrl :
                vappData.getSelectedSite().databaseServer.urlForPattern(fileSystemConfig.databaseUrlPattern);

        LogLevel logLevel = config.scriptMode ? LogLevel.DEBUG : LogLevel.INFO;
        DynamicLogger logger = new DynamicLogger(log);
        logger.log(logLevel, "Executing sql statement \"{}\" using database url {}", fileSystemConfig.sqlStatement, databaseUrl);
        Properties connectionProperties = determineConnectionProperties();
        log.debug("Connection properties: {}", connectionProperties.toString());

        DbUtils dbUtils = new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass, databaseUrl, connectionProperties);
        dbUtils.createConnection();

        List<Map> records = dbUtils.query(Map.class, fileSystemConfig.sqlStatement);
        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
        log.info(gson.toJson(records));
        log.debug("{} records for query {}", records.size(), fileSystemConfig.sqlStatement);
        dbUtils.closeConnection();
    }

}
