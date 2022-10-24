package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.IOUtils;
import com.vmware.util.input.InputListSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.util.input.InputUtils.readSelection;

@ActionDescription("Find TestNG classes run out of order on Jenkins when using TestNG in parallel")
public class FindTestClassesRunOutOfOrder extends BaseCommitAction {

    public FindTestClassesRunOutOfOrder(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        JobBuild build;
        if (jenkinsConfig.jobBuildNumber != null) {
            build = serviceLocator.getJenkins().getJobBuildDetails(jenkinsConfig.jenkinsJobsToUse, jenkinsConfig.jobBuildNumber);
        } else {
            List<JobBuild> jobBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
            if (jobBuilds.isEmpty()) {
                log.info("No job builds found in commit");
                return;
            }
            int selection = readSelection(jobBuilds.stream().map(InputListSelection.class::cast).collect(Collectors.toList()), "Select build");
            build = jobBuilds.get(selection);
        }
        log.info("Using job build {}", build.url);
        String consoleOutput = IOUtils.tail(build.logTextUrl(), jenkinsConfig.logLineCount);

        final Pattern startStringPattern = Pattern.compile("Starting setup for (.+?) using site (\\d+)");
        final Pattern endStringPattern = Pattern.compile("Finished cleanup for (.+?) using site (\\d+)");

        String[] lines = consoleOutput.split("\n");

        Map<String, List<String>> runningTestClasses = new HashMap<>();

        for (String line : lines) {
            Matcher startLineMatcher = startStringPattern.matcher(line);
            if (startLineMatcher.find()) {
                String className = startLineMatcher.group(1);
                String siteIndex = startLineMatcher.group(2);
                List<String> existingClassNames = runningTestClasses.computeIfAbsent(siteIndex, (key) -> new ArrayList<>());
                if (!existingClassNames.isEmpty()) {
                    log.info("Classes {} still in progress when starting {}", existingClassNames, className);
                }
                existingClassNames.add(className);
                continue;
            }
            Matcher endLineMatcher = endStringPattern.matcher(line);
            if (endLineMatcher.find()) {
                String className = endLineMatcher.group(1);
                String threadName = endLineMatcher.group(2);
                List<String> existingClassNames = runningTestClasses.computeIfAbsent(threadName, (key) -> new ArrayList<>());
                existingClassNames.remove(className);
            }
        }
    }
}
