package com.vmware.vcd.domain;

import com.vmware.util.input.InputListSelection;

public class QueryResultVMType extends ResourceType implements InputListSelection {
    public String container;
    public String containerName;
    public String status;
    public String ipAddress;

    public boolean isPoweredOn() {
        return "POWERED_ON".equalsIgnoreCase(status);
    }

    @Override
    public String getLabel() {
        return name + "(" + containerName + ") " + status;
    }

    public String getStatus() {
        return status;
    }
}
