package com.vmware.vcd.domain;

import com.google.gson.annotations.SerializedName;

@VcdMediaType("application/vnd.vmware.vcloud.leaseSettingsSection")
public class LeaseSection extends ResourceType {

    @SerializedName("_type")
    public String type = "LeaseSettingsSectionType";

    public long deploymentLeaseInSeconds;
}
