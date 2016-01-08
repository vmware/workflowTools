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
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (draft.jobBuilds.isEmpty()) {
            log.info("No need to strip jenkins jobs from commit as there are none");
        } else {
            log.info("Stripping jenkins jobs from commit");
            draft.jobBuilds.clear();
        }
    }
}
