package com.vmware.vcd.domain;

import java.util.Arrays;

@VcdMediaType("application/json")
public class QuotaPools {
    public QuotaPool[] quotaPools;


    public QuotaPool getRunningVmQuotaPool() {
        if (quotaPools == null || quotaPools.length == 0) {
            return null;
        }

        return Arrays.stream(quotaPools).filter(quotaPool ->
                quotaPool.quotaPoolDefinition.matches("urn:vcloud:legacy:vm", "count", "state==POWERED_ON"))
                .findFirst().orElse(null);
    }
}
