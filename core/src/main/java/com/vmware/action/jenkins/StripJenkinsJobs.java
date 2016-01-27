package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Strips jenkins build text from testing done section of commit.")
public class StripJenkinsJobs extends BaseCommitAction {

    public StripJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.jobBuilds.isEmpty()) {
            return "commit has no jobs";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        log.info("Stripping jenkins jobs from commit");
        draft.jobBuilds.clear();
    }
}
