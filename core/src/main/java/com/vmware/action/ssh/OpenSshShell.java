package com.vmware.action.ssh;

import java.io.BufferedInputStream;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;

@ActionDescription("Opems a ssh shell using the specified ssh site config.")
public class OpenSshShell extends BaseVappAction {

    public OpenSshShell(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SiteConfig siteConfigToUse = createSshSiteConfig();
        siteConfigToUse.validate();
        openSshShell(siteConfigToUse);
    }

    private void openSshShell(SiteConfig siteConfig) {
        log.info("Opening ssh shell for {}@{}", siteConfig.username, siteConfig.host);
        JSch jsch = new JSch();
        Session session = null;
        ChannelShell channel = null;
        try {
            session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", sshConfig.sshStrictHostChecking ? "yes" : "no");
            session.connect((int) TimeUnit.SECONDS.toMillis(30));

            channel = (ChannelShell) session.openChannel("shell");
            channel.setInputStream(new BufferedInputStream(System.in), true);
            channel.setOutputStream(System.out);
            channel.setPtySize(150, 24, 640, 600);
            channel.connect((int) TimeUnit.SECONDS.toMillis(30));

            waitForChannelToFinish(channel);
            log.info("Closed connection to {}@{}", siteConfig.username, siteConfig.host);
            System.exit(0);
        } catch (Exception e) {
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
