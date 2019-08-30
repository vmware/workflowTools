package com.vmware.vcd.domain;

import java.util.List;

import com.vmware.config.ssh.SiteConfig;

public class Sites {

    public List<Site> sites;

    public String firstCellUrl() {
        return sites.get(0).cells.get(0).endPointURI;
    }

    public SiteConfig firsCellSshConfig() {
        Cell cell = sites.get(0).cells.get(0);
        OvfProperties ovfProperties = cell.deployment.ovfProperties;
        return new SiteConfig(ovfProperties.hostname, 22, cell.osCredentials.username, cell.osCredentials.password);
    }

    private class Site {
        public List<Cell> cells;
    }

    private class Cell {
        public String name;

        public String endPointURI;

        public Deployment deployment;

        public OsCredentials osCredentials;
    }

    private class Deployment {
        public OvfProperties ovfProperties;
    }

    private class OsCredentials {
        public String username;

        public String password;
    }

    private class OvfProperties {
        public String hostname;
        public String user;
        public String password;
    }

}
