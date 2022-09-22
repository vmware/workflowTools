package com.vmware.vcd.domain;

import com.vmware.util.input.InputListSelection;

public class QueryResultVMType extends ResourceType implements InputListSelection, Sites.VmInfo {
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

    @Override
    public String getHost() {
        return ipAddress;
    }

    @Override
    public String getUiUrl() {
        return "https//" + ipAddress;
    }

    @Override
    public Sites.Credentials getSshCredentials() {
        return null;
    }

    @Override
    public Sites.Credentials getLoginCredentials() {
        return null;
    }

    @Override
    public String getUsernameInputId() {
        return null;
    }

    @Override
    public String getPasswordInputId() {
        return null;
    }

    @Override
    public String getLoginButtonLocator() {
        return null;
    }

    @Override
    public String getLoginButtonTestDescription() {
        return null;
    }

    @Override
    public String getLoggedInUrlPattern() {
        return null;
    }
}
