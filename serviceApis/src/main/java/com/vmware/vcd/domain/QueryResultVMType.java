package com.vmware.vcd.domain;

public class QueryResultVMType extends ResourceType {
    public String container;
    public String containerName;
    public String status;

    public boolean isPoweredOn() {
        return "POWERED_ON".equalsIgnoreCase(status);
    }
}
