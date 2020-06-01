package com.vmware.action.ssh;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;

@ActionDescription("Uses scp to copy a file from a ssh site.")
public class ScpFileFromRemote extends ExecuteSshCommand {
    public ScpFileFromRemote(WorkflowConfig config) {
        this(config, Arrays.asList("sourceFile", "destinationFile"));
    }

    public ScpFileFromRemote(WorkflowConfig config, List<String> failIfBlankProperties) {
        super(config);
        super.addFailWorkflowIfBlankProperties(failIfBlankProperties.toArray(new String[0]));
    }

    @Override
    public void process() {
        SiteConfig siteConfigToUse = createSshSiteConfig();
        siteConfigToUse.validate();
        copyFile(siteConfigToUse, fileSystemConfig.sourceFile, fileSystemConfig.destinationFile);
    }

    protected void copyFile(SiteConfig siteConfig, String sourceFile, String destinationFile) {
        log.info("Copying file {} from {}@{} to {}", sourceFile, siteConfig.username, siteConfig.host, destinationFile);
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;
        try {
            session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", sshConfig.sshStrictHostChecking ? "yes" : "no");
            session.connect((int) TimeUnit.SECONDS.toMillis(30));
            channel = session.openChannel("sftp");
            channel.connect();

            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.get(sourceFile, destinationFile);
            sftpChannel.exit();
        } catch (JSchException | SftpException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
