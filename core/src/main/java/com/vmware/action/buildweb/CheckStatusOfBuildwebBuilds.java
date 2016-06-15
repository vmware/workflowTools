
package com.vmware.action.buildweb;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Reads the testing done section and checks the status for all buiildweb builds found.")
public class CheckStatusOfBuildwebBuilds extends BaseCommitWithBuildwebBuildsAction {

    public CheckStatusOfBuildwebBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        buildweb.checkStatusOfBuilds(draft);
    }
}
