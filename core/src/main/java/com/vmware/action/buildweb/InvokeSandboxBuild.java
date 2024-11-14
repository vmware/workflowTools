package com.vmware.action.buildweb;

import com.vmware.BuildStatus;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.buildweb.domain.BuildwebId;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
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
        if (StringUtils.isEmpty(changelistId)) {
            changelistId = perforceClientConfig.changelistId;
        }
        if (StringUtils.isEmpty(changelistId)) {
            changelistId = InputUtils.readValueUntilNotBlank("Changelist id for sandbox");
        }
        String syncToParameter = " --syncto latest";
        String storeTreesParameter = buildwebConfig.storeTrees ? " --store-trees" : " --no-store-trees";
        String siteParameter = StringUtils.isNotBlank(buildwebConfig.buildwebSite) ? " --site=" + buildwebConfig.buildwebSite : "";
        String componentBuildsParameter = createComponentBuildsParameter();

        if (changelistId.toLowerCase().contains("head")) {
            String originalChangelistIdValue = changelistId;
            changelistId = git.revParse(changelistId);
            log.info("Assuming changelist id {} is a git ref, using commit ref {} as changeset value",
                    originalChangelistIdValue, changelistId);
            if (buildwebConfig.excludeSyncTo) {
                log.info("Excluding syncto parameter as excludeSyncTo is set to true");
                syncToParameter = ""; // rely on --accept-defaults to handle it correctly
            }
        }
        String command = format("%s sandbox queue %s --buildtype=%s%s --branch=%s --override-branch --changeset=%s%s%s%s --accept-defaults",
                buildwebConfig.goBuildBinPath, buildwebConfig.buildwebProject, buildwebConfig.buildType,
                syncToParameter, buildwebConfig.determineBuildwebBranch().getValue(),
                changelistId, storeTreesParameter, siteParameter, componentBuildsParameter);

        log.info("Invoking build {}", command);
        String output = CommandLineUtils.executeCommand(command, LogLevel.INFO);
        checkIfOutputContainsP4PasswordError(output);
        addBuildNumberInOutputToTestingDone(output);
    }

    private void checkIfOutputContainsP4PasswordError(String output) {
        if (output != null && output.contains("Perforce password (P4PASSWD) invalid or unset")) {
            throw new FatalException("Cannot invoke sandbox build as user is not logged into Perforce");
        }
    }

    private String createComponentBuildsParameter() {
        if (StringUtils.isEmpty(buildwebConfig.componentBuilds)) {
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
        String buildIdPattern = commitConfig.generateBuildWebIdPattern();

        String buildIdText = MatcherUtils.singleMatch(output, buildIdPattern);
        if (buildIdText != null) {
            BuildwebId buildwebId = new BuildwebId(buildIdText);
            String buildUrl = commitConfig.buildwebUrl + "/" + buildwebId.buildwebPath();
            log.info("Adding build {} to commit", buildUrl);
            String buildTypeUrl = commitConfig.buildwebUrl + "/" + buildwebId.getBuildType();
            Job sandboxJob = Job.buildwebJob(buildTypeUrl, buildwebConfig.buildDisplayName);
            draft.updateTestingDoneWithJobBuild(sandboxJob,
                    new JobBuild(sandboxJob.buildDisplayName, buildUrl, BuildStatus.BUILDING));
        } else {
            throw new RuntimeException("Unable to parse build url from output using pattern " + buildIdPattern);
        }
    }

}
