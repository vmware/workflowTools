package com.vmware.jenkins.domain;

import com.vmware.util.db.BaseDbClass;

public class JobViewMapping extends BaseDbClass {

    public JobViewMapping() {
    }

    public JobViewMapping(Long jobId, Long viewId) {
        this.jobId = jobId;
        this.viewId = viewId;
    }

    public Long jobId;

    public Long viewId;
}
