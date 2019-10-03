package com.vmware.vcd.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VappData {

    private List<QueryResultVappType> ownedVapps;

    private QueryResultVappType selectedVapp;

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

    public int getTestbedTemplateVmCount() {
        return testbedTemplateVmCount;
    }

    public void setTestbedTemplateVmCount(int testbedTemplateVmCount) {
        this.testbedTemplateVmCount = testbedTemplateVmCount;
    }
}
