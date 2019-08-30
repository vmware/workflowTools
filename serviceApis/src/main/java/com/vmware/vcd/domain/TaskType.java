package com.vmware.vcd.domain;

@VcdMediaType("application/vnd.vmware.vcloud.task")
public class TaskType extends ResourceType {

    public ReferenceType owner;

    public ReferenceType user;

    public ReferenceType organization;

    public String details;

    public String status;

    public String operation;
}
