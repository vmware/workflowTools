package com.vmware.action.ssh;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.StringUtils;

@ActionDescription(value = "Uses scp to copy a file from a ssh site. If destination file is IN_MEMORY_FILE the the contents are loaded into memory.",
        configFlagsToExcludeFromCompleter = {"--build-display-name", "--output-file", "--use-database-host", "--ssh-command"},
        configFlagsToAlwaysExcludeFromCompleter = "--ignore-unknown")
public class ScpFileFromRemote extends ExecuteSshCommand {

    private final String IN_MEMORY_FILE = "IN_MEMORY_FILE";

    public ScpFileFromRemote(WorkflowConfig config) {
        this(config, Collections.singletonList("sourceFile"));
    }

    public ScpFileFromRemote(WorkflowConfig config, List<String> failIfBlankProperties) {
        super(config);
        super.addFailWorkflowIfBlankProperties(failIfBlankProperties.toArray(new String[0]));
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(fileSystemConfig.destinationFile) && StringUtils.isEmpty(fileSystemConfig.outputVariableName)) {
            exitDueToFailureCheck("properties destinationFile and outputVariableName are both not set");
        }
        SiteConfig siteConfigToUse = createSshSiteConfig();
        siteConfigToUse.validate();
        copyFile(siteConfigToUse, fileSystemConfig.sourceFile, fileSystemConfig.destinationFile,fileSystemConfig.outputVariableName);
    }

    protected void copyFile(SiteConfig siteConfig, String sourceFile, String destinationFile, String outputVariableName) {
        if (StringUtils.isEmpty(destinationFile)) {
            log.info("Copying file {} from {}@{} as variable {}", sourceFile, siteConfig.username, siteConfig.host, outputVariableName);
        } else if (IN_MEMORY_FILE.equals(destinationFile)) {
            log.info("Copying file {} from {}@{} into memory", sourceFile, siteConfig.username, siteConfig.host);
        } else {
            log.info("Copying file {} from {}@{} to {}", sourceFile, siteConfig.username, siteConfig.host, destinationFile);
        }
        if (sshConfig.ignoreUnknownFile && StringUtils.isEmpty(sourceFile)) {
            log.info("Ignoring that source file file is empty as ignoreUnknownFile is set to true");
            return;
        } else if (StringUtils.isEmpty(sourceFile)) {
            exitDueToFailureCheck("source file is empty");
        }
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
            if (StringUtils.isEmpty(destinationFile)) {
                replacementVariables.addVariable(outputVariableName, readFile(sftpChannel, sourceFile));
            } else if (IN_MEMORY_FILE.equals(destinationFile)) {
                fileSystemConfig.fileData = readFile(sftpChannel, sourceFile);
            } else {
                sftpChannel.get(sourceFile, destinationFile);
            }
            sftpChannel.exit();
        } catch (UnsupportedEncodingException | JSchException | SftpException e) {
            if (sshConfig.ignoreUnknownFile && e.getMessage().contains("No such file")) {
                log.info("Ignoring that source file {} was not found", sourceFile);
                log.debug("Exception: {}", StringUtils.exceptionAsString(e));
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String readFile(ChannelSftp sftpChannel, String sourceFile) throws SftpException, UnsupportedEncodingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sftpChannel.get(sourceFile, outputStream);
        String fileData = outputStream.toString(StandardCharsets.UTF_8.name());
        log.trace("File data {}", fileData);
        return fileData;
    }
}
