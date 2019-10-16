package com.vmware.action.ssh;

import com.google.gson.Gson;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.ssh.SiteConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

public class OpenSshShellUsingVapp extends OpenSshShell {
    public OpenSshShellUsingVapp(WorkflowConfig config) {
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
        log.info("Selected Vapp {}", vappData.getSelectedVapp().name);
        Gson gson = new ConfiguredGsonBuilder().build();
        Sites vcdSites = gson.fromJson(draft.vappJsonForJenkinsJob, Sites.class);
        SiteConfig sshSiteConfig = vcdSites.siteSshConfig(vcdConfig.vcdSiteIndex, vcdConfig.vcdCellIndex);

        log.info("Site config {}", sshSiteConfig);
        sshSiteConfig.validate();
        openSshShell(sshSiteConfig);
    }


}
