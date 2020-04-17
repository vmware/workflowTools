package com.vmware.action.filesystem;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.stream.Stream;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Excecutes the specified sql statement against the configured database. Assumes that it is an update command.")
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
        Driver driver = createDatabaseDriver();

        String databaseUrl = vappData.getSelectedSite() != null ?
                vappData.getSelectedSite().databaseServer.urlForPattern(fileSystemConfig.databaseUrlPattern) : fileSystemConfig.databaseUrl;

        log.info("Executing sql statement \"{}\" using database url {}", fileSystemConfig.sqlStatement, databaseUrl);
        Properties connectionProperties = determineConnectionProperties();
        log.debug("Connection properties: {}", connectionProperties.toString());

        try (Connection sqlConnection = driver.connect(databaseUrl, connectionProperties)) {
            int rowsAffected = sqlConnection.createStatement().executeUpdate(fileSystemConfig.sqlStatement);
            log.info("{} affected", StringUtils.pluralize(rowsAffected, "row"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Driver createDatabaseDriver() {
        File databaseDriverFile = new File(fileSystemConfig.databaseDriverFile);
        Driver driver;
        try {
            URLClassLoader urlClassloader = new URLClassLoader( new URL[] { databaseDriverFile.toURI().toURL() }, System.class.getClassLoader() );
            Class driverClass = urlClassloader.loadClass(fileSystemConfig.databaseDriverClass);
            driver = (Driver) driverClass.newInstance();
        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return driver;
    }

    private Properties determineConnectionProperties() {
        Properties connectionProperties = new Properties();
        if (vappData.getSelectedSite() != null) {
            Sites.DatabaseServer databaseServer = vappData.getSelectedSite().databaseServer;
            connectionProperties.put("user", databaseServer.credentials.username);
            connectionProperties.put("password", databaseServer.credentials.password);
        } else {
            connectionProperties.put("user", fileSystemConfig.databaseUsername);
            connectionProperties.put("password", fileSystemConfig.databasePassword);
        }
        return connectionProperties;
    }


}
