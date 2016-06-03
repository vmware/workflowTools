package com.vmware.action.info;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.logging.Padder;

@ActionDescription("Displays the current workflow configuration as json.")
public class DisplayConfigAsJson extends BaseAction {

    private final Gson gson;

    public DisplayConfigAsJson(WorkflowConfig config) {
        super(config);
        gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
    }

    @Override
    public void process() {
        Padder titlePadder = new Padder("Workflow Configuration Json");
        titlePadder.infoTitle();
        log.info(gson.toJson(config));
        titlePadder.infoTitle();
    }
}
