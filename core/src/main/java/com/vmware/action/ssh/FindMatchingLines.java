package com.vmware.action.ssh;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Uses sed to list all matching lines for a ssh site.")
public class FindMatchingLines extends ExecuteSshCommand {
    public FindMatchingLines(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("logFile", "searchText");
    }

    @Override
    public void process() {
        sshConfig.sshCommand = String.format("grep -B %s -A %s --group-separator='' '%s' %s",
                sshConfig.lineCountBeforeMatch, sshConfig.lineCountAfterMatch, sshConfig.searchText, sshConfig.logFile);
        super.process();
    }
}
