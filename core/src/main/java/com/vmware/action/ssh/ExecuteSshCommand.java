package com.vmware.action.ssh;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
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
        if (StringUtils.isBlank(sshCommand)) {
            sshCommand = InputUtils.readValueUntilNotBlank("Ssh command");
        }
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
        log.info("Executing ssh command {} for {}@{}", command, siteConfig.username, siteConfig.host);
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", sshConfig.sshStrictHostChecking ? "yes" : "no");
            session.connect((int) TimeUnit.SECONDS.toMillis(30));

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            readCommandOutput(channel, command);
        } catch (JSchException e) {
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

    private void readCommandOutput(ChannelExec channel, String command) throws JSchException {
        channel.setErrStream(new LoggerOutputStream(channel, log, LogLevel.ERROR));
        channel.setOutputStream(new LoggerOutputStream(channel, log, LogLevel.INFO));

        Padder commandOutputPadder = new Padder("Command {} output", command);
        commandOutputPadder.infoTitle();
        channel.connect((int) TimeUnit.SECONDS.toMillis(5));
        waitForChannelToFinish(channel);
        commandOutputPadder.infoTitle();
    }

    private static class LoggerOutputStream extends OutputStream {
        private static final String FORCE_DISCONNECT_MESSAGE = "forceDisconnect";

        private ByteArrayOutputStream baus = new ByteArrayOutputStream(1000);
        private ChannelExec channel;
        private DynamicLogger logger;
        private LogLevel level;

        LoggerOutputStream(ChannelExec channel, Logger logger, LogLevel level) {
            this.channel = channel;
            this.logger = new DynamicLogger(logger);
            this.level = level;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                String logMessage = baus.toString();
                baus.reset();
                if (!forceDisconnect(logMessage)) {
                    logger.log(level, logMessage);
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
