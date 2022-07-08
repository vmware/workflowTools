package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.TableName;

@TableName("test_result")
public class NewTestResult extends BaseDbClass {
    @Expose(serialize = false, deserialize = false)
    public Integer[] failedBuildsTemp;

    @Expose(serialize = false, deserialize = false)
    public Integer[] skippedBuildsTemp;

    @Expose(serialize = false, deserialize = false)
    public Integer[] passedBuildsTemp;

    @Expose(serialize = false, deserialize = false)
    public Integer[] presumedPassedBuildsTemp;
}
