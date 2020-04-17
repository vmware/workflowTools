package com.vmware.action.ssh;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Tails the specified log file on the remote site")
public class TailLogFile extends ExecuteSshCommand {

    public TailLogFile(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("logFile");
    }

    @Override
    public void process() {
        String tailF = sshConfig.continuousTailing ? "f" : "";
        sshConfig.sshCommand = String.format("tail -%s%s %s", sshConfig.logLineCount, tailF, sshConfig.logFile);
        super.process();
    }
}
