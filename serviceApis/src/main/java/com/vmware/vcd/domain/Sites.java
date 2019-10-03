package com.vmware.vcd.domain;

import java.util.List;

import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.exception.FatalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sites {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public List<Site> sites;

    public String uiUrlForProvider() {
        Site firstSite = sites.get(0);
        if (firstSite.loadBalancer != null) {
            return firstSite.loadBalancer.endPointURI;
        }
        return firstSite.cells.get(0).endPointURI;
    }

    public SiteConfig siteSshConfig(int siteIndex, int cellIndex) {
        validateListSelection(sites, "vcd site index", siteIndex);
        validateListSelection(sites.get(siteIndex - 1).cells, "vcd cell index", cellIndex);

        log.debug("Using vcd site index {} and vcd cell index {}", siteIndex, cellIndex);
        Cell cell = sites.get(siteIndex - 1).cells.get(cellIndex - 1);
        OvfProperties ovfProperties = cell.deployment.ovfProperties;
        return new SiteConfig(ovfProperties.hostname, 22, cell.osCredentials.username, cell.osCredentials.password);
    }

    private void validateListSelection(List values, String propertyName, int selection) {
        if (selection < 1 || selection > values.size()) {
            throw new FatalException("{} selection {} is invalid. Should be between 1 and {}",
                    propertyName, String.valueOf(selection), String.valueOf(values.size()));
        }
    }

    private class Site {
        public Cell loadBalancer;
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
