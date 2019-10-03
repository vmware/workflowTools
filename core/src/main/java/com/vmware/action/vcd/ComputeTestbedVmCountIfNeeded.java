package com.vmware.action.vcd;

import java.io.File;
import java.util.Optional;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jenkins.Job;
import com.vmware.config.jenkins.JobParameter;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.scm.Perforce;

@ActionDescription("Computes the testbed to be deployed vm count if applicable")
public class ComputeTestbedVmCountIfNeeded extends BaseVappAction {
    public ComputeTestbedVmCountIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!vcdConfig.checkVmQuota) {
            return "checkVmQuota is set to false";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        int testbedTemplateVmCount = testbedTemplateVmCount();
        log.debug("Total VM count of {} for testbed templates", testbedTemplateVmCount);
        vappData.setTestbedTemplateVmCount(testbedTemplateVmCount);
    }

    private int testbedTemplateVmCount() {
        if (StringUtils.isBlank(vcdConfig.testbedTemplateDirectory)) {
            return 0;
        }

        final File testbedDirectory = determineTestbedTemplateDirectory();
        return config.getJenkinsJobsConfig().jobs().stream()
                .map(job -> countVmsUsedInJob(testbedDirectory, job)).reduce(Integer::sum).orElse(0);
    }

    private File determineTestbedTemplateDirectory() {
        File testbedDirectory = new File(vcdConfig.testbedTemplateDirectory);
        if (testbedDirectory.isAbsolute()) {
            return testbedDirectory;
        }

        File rootDirectory = git.workingDirectoryIsInGitRepo() ? git.getRootDirectory() : new File("");
        log.debug("Assuming path {} is relative, prepending root path {}", vcdConfig.testbedTemplateDirectory, rootDirectory.getPath());
        return new File(rootDirectory.getPath() + File.separator + vcdConfig.testbedTemplateDirectory);
    }

    private File determineRootDirectory() {
        File repoDirectory = git.getRootDirectory();
        if (repoDirectory != null) {
            return repoDirectory;
        } else {
            return serviceLocator.getPerforce().getWorkingDirectory();
        }
    }

    private int countVmsUsedInJob(File testbedDirectory, Job job) {
        Optional<JobParameter> matchingParameter =
                job.parameters.stream().filter(param -> param.name.equalsIgnoreCase(jenkinsConfig.testbedParameter)).findFirst();
        if (matchingParameter.isPresent() && !testbedDirectory.exists()) {
            throw new FatalException("Testbed template directory {} does not exist", vcdConfig.testbedTemplateDirectory);
        } else if (!matchingParameter.isPresent()) {
            return 0;
        }

        File testbedTemplateFile = new File(testbedDirectory.getPath() + File.separator + matchingParameter.get().value);
        if (!testbedTemplateFile.exists()) {
            throw new FatalException("Failed to find template file {}", testbedTemplateFile.getPath());
        }
        String templateText = IOUtils.read(testbedTemplateFile);
        int vmCount = StringUtils.occurenceCount(templateText, "\"deployment\":");
        log.info("Vm count of {} for testbed template {}", vmCount, matchingParameter.get().value);
        return vmCount;
    }
}
