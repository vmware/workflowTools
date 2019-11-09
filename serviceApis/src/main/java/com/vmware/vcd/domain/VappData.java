package com.vmware.vcd.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.util.exception.FatalException;

public class VappData {

    private List<QueryResultVappType> vapps;

    private QueryResultVappType selectedVapp;

    private Sites.Site selectedSite;

    private Sites.DeployedVM selectedVcdCell;

    private int testbedTemplateVmCount;

    public VappData () {
        this.vapps = new ArrayList<>();
    }

    public void setVapps(List<QueryResultVappType> vapps) {
        this.vapps = vapps;
    }

    public List<QueryResultVappType> getVapps() {
        return vapps;
    }

    public void setSelectedVapp(QueryResultVappType selectedVapp) {
        this.selectedVapp = selectedVapp;
    }

    public QueryResultVappType getSelectedVapp() {
        return selectedVapp;
    }

    public List<String> vappLabels() {
        List<String> values = vapps.stream()
                .map(QueryResultVappType::getLabel).collect(Collectors.toList());
        values.add("None");
        return values;
    }

    public int poweredOnVmCount() {
        return getVapps().stream().filter(QueryResultVappType::isOwnedByWorkflowUser)
                .map(QueryResultVappType::poweredOnVmCount).reduce(Integer::sum).orElse(0);
    }

    public void setSelectedVappByIndex(int index) {
        if (index < vapps.size()) {
            this.setSelectedVapp(vapps.get(index));
        }
    }

    public void setSelectedVappByName(String name) {
        List<String> vappNames = vapps.stream().map(vapp -> vapp.name).collect(Collectors.toList());
        int vappIndex = vappNames.indexOf(name);
        if (vappIndex == -1) {
            throw new FatalException("Vapp name {} not found in vapp list {}", name, vappNames.toString());
        }
        setSelectedVapp(vapps.get(vappIndex));
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
