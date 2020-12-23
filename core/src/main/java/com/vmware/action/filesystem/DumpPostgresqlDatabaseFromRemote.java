package com.vmware.action.filesystem;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import com.vmware.action.ssh.ScpFileFromRemote;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.exception.FatalException;
import com.vmware.vcd.domain.Sites;

@ActionDescription(value = "Uses psql to copy a database from a source server to a destination server",
        configFlagsToExcludeFromCompleter = {"--build-display-name", "--output-file", "--use-database-host", "--source-file"})
public class DumpPostgresqlDatabaseFromRemote extends ScpFileFromRemote {
    public DumpPostgresqlDatabaseFromRemote(WorkflowConfig config) {
        super(config, Collections.singletonList("destinationFile"));
    }

    @Override
    public void process() {
        SiteConfig sourceDatabaseSiteConfig = constructSourceSiteConfig();
        String sourceSchemaName = sshConfig.hasConfiguredSourceDatabaseServer()
                ? sshConfig.sourceDatabaseSchemaName : vappData.getSelectedSite().databaseServer.dbname;
        String outputFile = sourceSchemaName + new SimpleDateFormat("_yyyyddhh_HHmmss").format(new Date()) + ".sql";
        executeSshCommand(sourceDatabaseSiteConfig, String.format("pg_dump -U postgres -d %s -f %s", sourceSchemaName, outputFile));
        copyFile(sourceDatabaseSiteConfig, outputFile, fileSystemConfig.destinationFile);
        executeSshCommand(sourceDatabaseSiteConfig, "rm -f " + outputFile);
    }

    private SiteConfig constructSourceSiteConfig() {
        if (sshConfig.hasConfiguredSourceDatabaseServer()) {
            return sshConfig.commandLineSite();
        } else {
            Sites.DatabaseServer databaseServer = vappData.getSelectedSite().databaseServer;
            if (!"postgres".equalsIgnoreCase(databaseServer.databaseType)) {
                throw new FatalException("Database type should be postgres, found " + databaseServer.databaseType);
            }
            return new SiteConfig(databaseServer.host, 22, "root", databaseServer.deployment.guestProperties.adminPassword);
        }
    }
}
