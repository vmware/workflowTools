package com.vmware.action.buildweb;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.CommitConfiguration;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

import static java.lang.String.format;

@ActionDescription("Used to invoke a sandbox build on buildweb. This is a VMware specific action.")
public class InvokeSandboxBuild extends BaseCommitAction {

    public InvokeSandboxBuild(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistId = draft.perforceChangelistId;
        if (StringUtils.isBlank(changelistId)) {
            changelistId = InputUtils.readValueUntilNotBlank("Changelist id for sandbox");
        }

        String[] inputs = new String[] {"opt", "", ""};
        String[] textsToWaitFor = new String[] {
                "Buildtype to use [beta]:", config.buildwebProject + "?",
                format("queued (%s/%s)", config.buildwebProject, config.buildwebBranch)};
        String command = format("%s sandbox queue %s --syncto latest --branch=%s --changeset=%s",
                config.goBuildBinPath, config.buildwebProject, config.buildwebBranch, changelistId);

        String output = CommandLineUtils.executeScript(command, inputs, textsToWaitFor, LogLevel.INFO);
        CommitConfiguration commitConfig = config.getCommitConfiguration();
        String buildNumberPattern = commitConfig.generateBuildWebNumberPattern();

        String buildNumber = MatcherUtils.singleMatch(output, buildNumberPattern);
        if (buildNumber != null) {
            String buildUrl = commitConfig.buildWebUrl() + "/" + buildNumber;
            log.info("Adding build {} to commit", buildUrl);
            draft.updateTestingDoneWithJobBuild(commitConfig.buildWebUrl(),
                    new JobBuild(buildUrl, BuildResult.BUILDING));
        } else {
            log.warn("Unable to parse build url using pattern {}", buildNumberPattern);
        }
    }

}
