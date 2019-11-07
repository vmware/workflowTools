package com.vmware.vcd.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vmware.config.ssh.SiteConfig;
import com.vmware.util.exception.FatalException;

public class VappData {

    private List<QueryResultVappType> ownedVapps;

    private QueryResultVappType selectedVapp;

    private Sites.Site selectedSite;

    private Sites.DeployedVM selectedVcdCell;

    private int testbedTemplateVmCount;

    public VappData () {
        this.ownedVapps = new ArrayList<>();
    }

    public void setOwnedVapps(List<QueryResultVappType> ownedVapps) {
        this.ownedVapps = ownedVapps;
    }

    public List<QueryResultVappType> getOwnedVapps() {
        return ownedVapps;
    }

    public void setSelectedVapp(QueryResultVappType selectedVapp) {
        this.selectedVapp = selectedVapp;
    }

    public QueryResultVappType getSelectedVapp() {
        return selectedVapp;
    }

    public List<String> ownedVappLabels() {
        List<String> values = ownedVapps.stream().map(QueryResultVappType::getLabel).collect(Collectors.toList());
        values.add("None");
        return values;
    }

    public int poweredOnVmCount() {
        return getOwnedVapps().stream()
                .map(QueryResultVappType::poweredOnVmCount).reduce(Integer::sum).orElse(0);
    }

    public void setSelectedVappByIndex(int index) {
        if (index < ownedVapps.size()) {
            this.setSelectedVapp(ownedVapps.get(index));
        }
    }

    public void setSelectedVappByName(String name) {
        List<String> vappNames = ownedVapps.stream().map(vapp -> vapp.name).collect(Collectors.toList());
        int vappIndex = vappNames.indexOf(name);
        if (vappIndex == -1) {
            throw new FatalException("Vapp name {} not found in vapp list {}", name, vappNames.toString());
        }
        setSelectedVapp(ownedVapps.get(vappIndex));
    }

    public int getTestbedTemplateVmCount() {
        return testbedTemplateVmCount;
    }

    public void setTestbedTemplateVmCount(int testbedTemplateVmCount) {
        this.testbedTemplateVmCount = testbedTemplateVmCount;
    }

    public Sites.Site getSelectedSite() {
        return selectedSite;
    }

    public void setSelectedSite(Sites.Site selectedSite) {
        this.selectedSite = selectedSite;
    }

    public Sites.DeployedVM getSelectedVcdCell() {
        return selectedVcdCell;
    }

    public void setSelectedVcdCell(Sites.DeployedVM selectedVcdCell) {
        this.selectedVcdCell = selectedVcdCell;
    }
}
