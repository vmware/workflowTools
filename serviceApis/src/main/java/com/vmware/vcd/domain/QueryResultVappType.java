package com.vmware.vcd.domain;

import java.util.Date;

import com.google.gson.annotations.Expose;
import com.vmware.util.input.InputListSelection;

public class QueryResultVappType extends ResourceType implements InputListSelection {
    public String ownerName;

    @Expose(serialize = false, deserialize = false)
    private boolean isOwnedByWorkflowUser;

    public String status;

    public OtherAttributes otherAttributes;

    public int poweredOnVmCount() {
        if (!"POWERED_ON".equalsIgnoreCase(status)) {
            return 0;
        } else {
            return otherAttributes.numberOfVMs;
        }
    }

    @Override
    public String getLabel() {
        String label = name + " (" + status + ") VM Count: " + otherAttributes.numberOfVMs + " Expires: " + otherAttributes.autoUndeployDate;
        if (!isOwnedByWorkflowUser) {
            label = "Shared, owner " +ownerName + " - " + label;
        }
        return label;
    }

    public boolean isOwnedByWorkflowUser() {
        return isOwnedByWorkflowUser;
    }

    public void setOwnedByWorkflowUser(boolean ownedByWorkflowUser) {
        isOwnedByWorkflowUser = ownedByWorkflowUser;
    }

    public class OtherAttributes {
        public int numberOfVMs;

        public Date autoUndeployDate;
    }
}
