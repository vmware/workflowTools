package com.vmware.vcd.domain;

import java.util.List;

@VcdMediaType("application/vnd.vmware.vcloud.query.records")
public class QueryResultVappsType extends ResourceType {

    public List<QueryResultVappType> record;
}
