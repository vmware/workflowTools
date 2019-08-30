package com.vmware.action.ssh;

import com.google.gson.Gson;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Executes a ssh command against the selected Vapp")
public class ExecuteSshCommandUsingVapp extends ExecuteSshCommand {

    public ExecuteSshCommandUsingVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.vappJsonForJenkinsJob)) {
            return "no Vapp json loaded";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        log.info("Selected Vapp {}", vappData.getSelectedVapp());
        Gson gson = new ConfiguredGsonBuilder().build();
        Sites vcdSites = gson.fromJson(draft.vappJsonForJenkinsJob, Sites.class);
        SiteConfig sshSiteConfig = vcdSites.firsCellSshConfig();
        log.info("Site config {}", sshSiteConfig);
        sshSiteConfig.validate();

        String sshCommand = sshConfig.sshCommand;
        if (StringUtils.isBlank(sshCommand)) {
            sshCommand = InputUtils.readValueUntilNotBlank("Ssh command");
        }

        sshCommand = expandParametersInCommand(sshCommand);
        executeSshCommand(sshSiteConfig, sshCommand);
    }
}
