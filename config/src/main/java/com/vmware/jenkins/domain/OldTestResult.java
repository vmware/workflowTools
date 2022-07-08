package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.TableName;

@TableName("test_result")
public class OldTestResult extends BaseDbClass {
    @Expose(serialize = false, deserialize = false)
    public String failedBuilds;

    @Expose(serialize = false, deserialize = false)
    public String skippedBuilds;

    @Expose(serialize = false, deserialize = false)
    public String passedBuilds;

    @Expose(serialize = false, deserialize = false)
    public String presumedPassedBuilds;

}
