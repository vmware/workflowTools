package com.vmware.vcd.domain;

import java.util.Date;

import com.vmware.util.input.InputListSelection;

public class QueryResultVappType extends ResourceType implements InputListSelection {
    public String ownerName;

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
        return name + " (" + status + ") VM Count: " + otherAttributes.numberOfVMs + " Expires: " + otherAttributes.autoUndeployDate;
    }

    public class OtherAttributes {
        public int numberOfVMs;

        public Date autoUndeployDate;
    }
}
