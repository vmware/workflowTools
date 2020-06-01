package com.vmware.jenkins.domain;

import com.vmware.config.jenkins.JobParameter;

public class JobBuildAction {

    public JobParameter[] parameters;

    public JobBuildRevision lastBuiltRevision;

    public JobBuildCause[] causes;

}
