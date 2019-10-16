package com.vmware.action.ssh;

import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.vmware.action.base.BaseVappAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.ThreadUtils;

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

    protected void openSshShell(SiteConfig siteConfig) {
        log.info("Opening ssh shell for {}@{}", siteConfig.username, siteConfig.host);
        JSch jsch = new JSch();
        try {
            Session session = jsch.getSession(siteConfig.username, siteConfig.host, siteConfig.portNumber());
            session.setPassword(siteConfig.password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect((int) TimeUnit.SECONDS.toMillis(30));

            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setInputStream(new BufferedInputStream(System.in), true);
            channel.setOutputStream(System.out);
            channel.setPtySize(150, 24, 640, 600);
            channel.connect((int) TimeUnit.SECONDS.toMillis(30));

            while (channel.isConnected()) {
                ThreadUtils.sleep(1, TimeUnit.SECONDS);
            }
            channel.disconnect();
            session.disconnect();
            log.info("Closed connection to {}@{}", siteConfig.username, siteConfig.host);
            System.exit(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
