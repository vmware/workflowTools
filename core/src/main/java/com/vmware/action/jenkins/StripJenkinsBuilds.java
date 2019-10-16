package com.vmware.action.jenkins;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Strips jenkins build text from testing done section of commit.")
public class StripJenkinsBuilds extends BaseCommitWithJenkinsBuildsAction {

    public StripJenkinsBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Stripping jenkins builds from commit");
        for (JobBuild jobBuild : draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl)) {
            draft.jobBuilds.remove(jobBuild);
        }
    }
}
