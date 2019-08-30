package com.vmware.action.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vmware.JobBuild;
import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jenkins.Job;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@ActionDescription("Executes the specified ssh command against the specified ssh site.")
public class ExecuteSshCommand extends BaseVappAction {

    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";

    public ExecuteSshCommand(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SiteConfig siteConfigToUse = createSiteConfig();
        siteConfigToUse.validate();

        String sshCommand = sshConfig.sshCommand;
        if (StringUtils.isBlank(sshCommand)) {
            sshCommand = InputUtils.readValueUntilNotBlank("Ssh command");
        }

        sshCommand = expandParametersInCommand(sshCommand);

        executeSshCommand(siteConfigToUse, sshCommand);
    }

    private SiteConfig createSiteConfig() {
        if (sshConfig.useSshSite()) {
            String sshSite = sshConfig.sshSite;
            TreeMap<String, SiteConfig> sshSiteConfigs = sshConfig.sshSiteConfigs;
            if (StringUtils.isBlank(sshSite)) {
                sshSite = InputUtils.readValueUntilNotBlank("Ssh site", sshSiteConfigs.keySet());
            }
            if (!sshSiteConfigs.containsKey(sshSite)) {
                throw new FatalException("Ssh site {} is not present in list {}", sshSite, sshSiteConfigs.keySet().toString());
            }
            return sshSiteConfigs.get(sshSite);
        } else {
            return sshConfig.commandLineSite();
        }
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
        log.info("Executing ssh command {}", command);
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;
        try {
            session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setServerAliveInterval((int) TimeUnit.SECONDS.toMillis(5));
            session.connect((int) TimeUnit.SECONDS.toMillis(30));

            channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            channel.setInputStream(null);

            readCommandOutput(channel, command);
        } catch (JSchException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

    }

    private void readCommandOutput(Channel channel, String command) throws IOException, JSchException {
        InputStream in = channel.getInputStream();
        channel.connect((int) TimeUnit.SECONDS.toMillis(5));
        Padder commandOutputPadder = new Padder("Command {} output", command);
        commandOutputPadder.infoTitle();
        IOUtils.read(in, LogLevel.INFO);
        commandOutputPadder.infoTitle();
    }


}
