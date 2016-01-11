package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

public abstract class BaseCommitReadAction extends BaseCommitAction {

    protected String title;

    protected Field property;

    public BaseCommitReadAction(WorkflowConfig config, String propertyName) throws NoSuchFieldException {
        super(config);
        this.title = StringUtils.splitOnCapitalization(propertyName);
        this.property = ReviewRequestDraft.class.getField(propertyName);
    }

    @Override
    public String cannotRunAction() {
        String propertyValue;
        try {
            propertyValue = (String) property.get(draft);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (!config.setEmptyPropertiesOnly || StringUtils.isBlank(propertyValue)) {
            return super.cannotRunAction();
        }

        return "setEmptyPropertiesOnly is set to true and " + title + " has a value";
    }


}
