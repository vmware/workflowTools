package com.vmware.action.ssh;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import org.slf4j.Logger;

@ActionDescription("Executes the specified ssh command against the specified ssh site.")
public class ExecuteSshCommand extends BaseVappAction {

    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";

    public ExecuteSshCommand(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SiteConfig siteConfigToUse = createSshSiteConfig();
        siteConfigToUse.validate();

        String sshCommand = sshConfig.sshCommand;
        if (StringUtils.isEmpty(sshCommand)) {
            sshCommand = InputUtils.readValueUntilNotBlank("Ssh command");
        }
        sshCommand = expandParametersInCommand(sshCommand);
        executeSshCommand(siteConfigToUse, sshCommand);
    }

    protected String expandParametersInCommand(String sshCommand) {
        List<String> parameterNamesInCommand = MatcherUtils.allMatches(sshCommand, "(\\$[_\\w]+)");
        for (String parameterName : parameterNamesInCommand) {
            if (parameterName.equals(SANDBOX_BUILD_NUMBER)) {
                sshCommand = sshCommand.replace(parameterName, determineSandboxBuildNumber(buildwebConfig.buildDisplayName));
            } else {
                String label = parameterName.length() < 2 ? parameterName :
                        parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1).toLowerCase();
                label = label.replace("_", " ");
                sshCommand = sshCommand.replace(parameterName, InputUtils.readValueUntilNotBlank(label));
            }
        }
        return sshCommand;
    }

    protected void executeSshCommand(SiteConfig siteConfig, String command) {
        log.info("Executing ssh command for {}@{}", siteConfig.username, siteConfig.host);
        log.info("{}", command);
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", sshConfig.sshStrictHostChecking ? "yes" : "no");
            session.connect((int) TimeUnit.SECONDS.toMillis(30));

            channel = (ChannelExec) session.openChannel("exec");
            channel.setInputStream(null);
            channel.setCommand(command);

            readCommandOutput(channel, command);
        } catch (JSchException | IOException e) {
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

    private void readCommandOutput(ChannelExec channel, String command) throws JSchException, IOException {
        BufferedWriter writer = outputWriter();

        channel.setErrStream(new LoggerOutputStream(channel, log, LogLevel.ERROR, null));
        channel.setOutputStream(new LoggerOutputStream(channel, log, LogLevel.INFO, writer));

        Padder commandOutputPadder = new Padder("Command {} output", command);
        commandOutputPadder.infoTitle();
        channel.connect((int) TimeUnit.SECONDS.toMillis(5));
        waitForChannelToFinish(channel);
        commandOutputPadder.infoTitle();
        if (writer != null) {
           writer.close();
        }
    }

    private static class LoggerOutputStream extends OutputStream {
        private static final String FORCE_DISCONNECT_MESSAGE = "forceDisconnect";
        private final BufferedWriter writer;

        private ByteArrayOutputStream baus = new ByteArrayOutputStream(1000);
        private ChannelExec channel;
        private DynamicLogger logger;
        private LogLevel level;

        LoggerOutputStream(ChannelExec channel, Logger logger, LogLevel level, BufferedWriter writer) {
            this.channel = channel;
            this.logger = new DynamicLogger(logger);
            this.level = level;
            this.writer = writer;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                String logMessage = baus.toString();
                baus.reset();
                if (!forceDisconnect(logMessage)) {
                    logOrPrintOutput(logMessage);
                }
            } else {
                baus.write(b);
            }
        }

        @Override
        public void flush() {
            if (baus.size() <= 0) {
                return;
            }

            String logMessage = baus.toString();
            if (!forceDisconnect(logMessage)) {
                logOrPrintOutput(logMessage);
            }
        }

        private void logOrPrintOutput(String logMessage) {
            if (writer != null) {
                try {
                    writer.write(logMessage);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            } else {
                logger.log(level, logMessage);
            }
        }

        private boolean forceDisconnect (String logMessage) {
            if (FORCE_DISCONNECT_MESSAGE.equals(logMessage)) {
                logger.log(LogLevel.INFO, "Closing connection as echo message {} detected", FORCE_DISCONNECT_MESSAGE);
                channel.disconnect();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void close() {
            baus = null;
        }
    }
}
