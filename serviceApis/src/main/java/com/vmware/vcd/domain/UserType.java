package com.vmware.vcd.domain;

@VcdMediaType("application/vnd.vmware.admin.user")
public class UserType {

    public String id;
    public String name;
    public String fullName;
    public int storedVmQuota;
    public int deployedVmQuota;

}
