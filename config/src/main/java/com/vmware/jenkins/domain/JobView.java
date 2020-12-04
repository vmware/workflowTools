package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;

public class JobView extends BaseDbClass {
    public String name;

    public String url;

    @Expose(serialize = false, deserialize = false)
    public int lastFetchAmount;

    @DbSaveIgnore
    public Job[] jobs;
}
