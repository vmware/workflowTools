package com.vmware.action.info;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.SystemUtils;

@ActionDescription("Copies the Workflow config in json format to the clipboard")
public class CopyConfigToClipboard extends BaseAction {
    private final Gson gson;
    public CopyConfigToClipboard(WorkflowConfig config) {
        super(config);
        gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
    }

    @Override
    public void process() {
        log.info("Copying workflow config in json format to clipboard");
        SystemUtils.copyTextToClipboard(gson.toJson(config));
    }
}
