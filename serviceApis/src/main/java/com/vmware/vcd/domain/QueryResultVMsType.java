package com.vmware.vcd.domain;

import java.util.List;

@VcdMediaType("application/vnd.vmware.vcloud.query.records")
public class QueryResultVMsType extends ResourceType {

    public List<QueryResultVMType> record;
}
