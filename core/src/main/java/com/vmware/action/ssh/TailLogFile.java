package com.vmware.action.ssh;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Tails the specified log file on the remote site")
public class TailLogFile extends ExecuteSshCommand {

    public TailLogFile(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (StringUtils.isEmpty(sshConfig.logFile)) {
            exitDueToFailureCheck("no log file specified");
        }
    }

    @Override
    public void process() {
        String tailF = sshConfig.continuousTailing ? "f" : "";
        sshConfig.sshCommand = String.format("tail -%s%s %s", sshConfig.logLineCount, tailF, sshConfig.logFile);
        super.process();
    }
}
