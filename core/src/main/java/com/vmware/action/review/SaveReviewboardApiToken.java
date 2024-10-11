package com.vmware.action.review;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Saves an api token for reviewboard")
public class SaveReviewboardApiToken extends BaseAction {
    public SaveReviewboardApiToken(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String apiToken = InputUtils.readValue("Enter API token (Create in UI under My Account -> Authentication -> API Tokens");
        serviceLocator.getReviewBoard().saveApiToken(apiToken);
    }
}
