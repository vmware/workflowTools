package com.vmware.action.buildweb;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jenkins.Job;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;

import java.util.regex.Pattern;

import static java.lang.String.format;

@ActionDescription("Used to invoke a sandbox build on buildweb. This is a VMware specific action.")
public class InvokeSandboxBuild extends BaseCommitAction {

    private static final String SANDBOX_BUILD_PREFIX = "$SANDBOX_";

    public InvokeSandboxBuild(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistId = draft.perforceChangelistId;
        if (StringUtils.isBlank(changelistId)) {
            changelistId = perforceClientConfig.changelistId;
        }
        if (StringUtils.isBlank(changelistId)) {
            changelistId = InputUtils.readValueUntilNotBlank("Changelist id for sandbox");
        }
        String syncToParameter = " --syncto latest";
        String storeTreesParamter = buildwebConfig.storeTrees ? " --store-trees" : "";
        String componentBuildsParameter = createComponentBuildsParameter();

        if (changelistId.toLowerCase().contains("head")) {
            log.info("Assuming changelist id {} is a git ref, using tracking branch {} as syncTo value",
                    changelistId, gitRepoConfig.trackingBranchPath());
            changelistId = git.revParse(changelistId);
            syncToParameter = ""; // --accept-defaults handles it correctly
        }
        String command = format("%s sandbox queue %s --buildtype=%s%s --branch=%s --override-branch --changeset=%s%s%s --accept-defaults",
                buildwebConfig.goBuildBinPath, buildwebConfig.buildwebProject, buildwebConfig.buildType,
                syncToParameter, buildwebConfig.buildwebBranch,
                changelistId, storeTreesParamter, componentBuildsParameter);

        log.info("Invoking {} build {}", buildwebConfig.buildDisplayName, command);
        String output = CommandLineUtils.executeCommand(command, LogLevel.INFO);
        addBuildNumberInOutputToTestingDone(output);
    }

    private String createComponentBuildsParameter() {
        if (StringUtils.isBlank(buildwebConfig.componentBuilds)) {
            return "";
        }
        String componentBuilds = " --component-builds " + buildwebConfig.componentBuilds;
        componentBuilds = componentBuilds.replace(",", "=");
        if (componentBuilds.contains(SANDBOX_BUILD_PREFIX)) {
            String expectedBuildDisplayName = MatcherUtils.singleMatchExpected(componentBuilds, Pattern.quote(SANDBOX_BUILD_PREFIX) + "([\\w_]+)");
            String buildNumber = determineSandboxBuildNumber(expectedBuildDisplayName);
            if (StringUtils.isInteger(buildNumber)) {
                buildNumber = "sb-" + buildNumber;
            }
            componentBuilds = componentBuilds.replace(SANDBOX_BUILD_PREFIX + expectedBuildDisplayName, buildNumber);
        }
        return componentBuilds;
    }

    private void addBuildNumberInOutputToTestingDone(String output) {
        String buildNumberPattern = commitConfig.generateBuildWebNumberPattern();

        String buildNumber = MatcherUtils.singleMatch(output, buildNumberPattern);
        if (buildNumber != null) {
            String buildUrl = commitConfig.buildWebUrl() + "/" + buildNumber;
            log.info("Adding build {} to commit", buildUrl);
            Job sandboxJob = Job.sandboxJob(commitConfig.buildWebUrl(), buildwebConfig.buildDisplayName);
            draft.updateTestingDoneWithJobBuild(sandboxJob, new JobBuild(sandboxJob.jobDisplayName, buildUrl, BuildResult.BUILDING));
        } else {
            throw new RuntimeException("Unable to parse build url from output using pattern " + buildNumberPattern);
        }
    }

}
