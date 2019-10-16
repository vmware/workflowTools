package com.vmware.vcd.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sites {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public List<Site> sites;

    public String vcServerUrl(Integer siteIndex) {
        Site selectedSite = determineSite(siteIndex);
        if (selectedSite.vcServers.size() == 1) {
            log.info("Using first VCenter {} as there is only one VCenter", selectedSite.vcServers.get(0).name);
            return selectedSite.vcServers.get(0).endPointURI;
        } else {
            List<InputListSelection> vcValues = selectedSite.vcServers.stream().map(vc -> ((InputListSelection) vc)).collect(Collectors.toList());
            int selection = InputUtils.readSelection(vcValues, "Select VCenter");
            return selectedSite.vcServers.get(selection).endPointURI;
        }
    }

    public String uiUrl(Integer siteIndex, Integer cellIndex) {
        Site selectedSite = determineSite(siteIndex);
        if (selectedSite.loadBalancer != null) {
            log.info("Using loadbalancer url {}", selectedSite.loadBalancer.endPointURI);
            return selectedSite.loadBalancer.endPointURI;
        }
        Cell cell = determineCell(selectedSite, cellIndex);
        return cell.endPointURI;
    }

    public SiteConfig siteSshConfig(Integer siteIndex, Integer cellIndex) {
        Site site = determineSite(siteIndex);

        Cell cell = determineCell(site, cellIndex);
        OvfProperties ovfProperties = cell.deployment.ovfProperties;
        return new SiteConfig(ovfProperties.hostname, 22, cell.osCredentials.username, cell.osCredentials.password);
    }

    private Cell determineCell(Site site, Integer cellIndex) {
        if (cellIndex == null && site.cells.size() == 1) {
            log.info("Using first cell as there is only one cell");
            cellIndex = 0;
        } else if (cellIndex == null) {
            List<String> cellChoices = site.cells.stream().map(cell -> cell.name).collect(Collectors.toList());
            cellIndex = InputUtils.readSelection(cellChoices, "Select Cell");
        } else {
            log.info("Using specified cell index of {}", cellIndex);
            validateListSelection(site.cells, "vcd cell index", cellIndex);
            cellIndex--; // subtract one to match zero indexed list
        }
        return site.cells.get(cellIndex);
    }

    private Site determineSite(Integer siteIndex) {
        if (siteIndex == null && sites.size() == 1) {
            log.info("Using first site as there is only one site");
            siteIndex = 0;
        } else if (siteIndex == null) {
            siteIndex = InputUtils.readSelection(IntStream.range(0, sites.size()).mapToObj(String::valueOf).collect(Collectors.toList()), "Select Site");
        } else {
            log.info("Using specified site index of {}", siteIndex);
            validateListSelection(sites, "vcd site index", siteIndex);
            siteIndex--; // subtract one to match zero indexed list
        }
        return sites.get(siteIndex);
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
        public List<Cell> vcServers;
    }

    private class Cell implements InputListSelection {
        public String name;

        public String endPointURI;

        public Deployment deployment;

        public OsCredentials osCredentials;

        @Override
        public String getLabel() {
            return name + " (" + endPointURI + ")";
        }
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
