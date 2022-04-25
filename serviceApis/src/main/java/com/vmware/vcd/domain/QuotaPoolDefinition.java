package com.vmware.vcd.domain;

import java.util.Arrays;

public class QuotaPoolDefinition {
    public String quotaResourceName;
    public String resourceType;
    public String quotaResourceUnit;
    public int quota;
    public String[] qualifiers;

    public boolean matches(String quotaResourceType, String quotaResourceUnit, String qualifier) {
        if (qualifiers == null) {
            return false;
        }

        return resourceType.equals(quotaResourceType) && this.quotaResourceUnit.equals(quotaResourceUnit) && Arrays.asList(qualifiers).contains(qualifier);
    }
}
