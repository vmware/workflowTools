package com.vmware.action.filesystem;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Stream;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.db.DbUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Executes the specified sql statement against the configured database. Assumes that it is an update command.")
public class ExecuteSqlStatement extends BaseVappAction {

    public ExecuteSqlStatement(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("databaseDriverClass", "databaseDriverFile", "sqlStatement");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (vappData.getSelectedSite() != null) {
            super.skipActionIfUnset("databaseUrlPattern");
        } else {
            Stream.of("databaseUrl", "databaseUsername", "databasePassword").forEach(this::skipActionIfUnset);
        }
    }

    @Override
    public void process() {
        String databaseUrl = fileSystemConfig.databaseConfigured() ? fileSystemConfig.databaseUrl :
                vappData.getSelectedSite().databaseServer.urlForPattern(fileSystemConfig.databaseUrlPattern);

        log.info("Executing sql statement \"{}\" using database url {}", fileSystemConfig.sqlStatement, databaseUrl);
        Properties connectionProperties = determineConnectionProperties();
        log.debug("Connection properties: {}", connectionProperties.toString());

        DbUtils dbUtils = new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass, databaseUrl, connectionProperties);

        try (Connection sqlConnection = dbUtils.createConnection()) {
            int rowsAffected = sqlConnection.createStatement().executeUpdate(fileSystemConfig.sqlStatement);
            log.info("{} affected", StringUtils.pluralize(rowsAffected, "row"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private Properties determineConnectionProperties() {
        if (fileSystemConfig.databaseConfigured()) {
            Properties properties = fileSystemConfig.dbConnectionProperties();
            properties.remove("url");
            return properties;
        }
        Properties connectionProperties = new Properties();
        Sites.DatabaseServer databaseServer = vappData.getSelectedSite().databaseServer;
        connectionProperties.put("user", databaseServer.credentials.username);
        connectionProperties.put("password", databaseServer.credentials.password);
        return connectionProperties;
    }


}
