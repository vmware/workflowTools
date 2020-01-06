package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;

import java.lang.reflect.Field;

public abstract class BaseCommitReadAction extends BaseCommitAction {

    protected String title;

    protected Field property;

    public BaseCommitReadAction(WorkflowConfig config, String propertyName) {
        super(config);
        this.title = StringUtils.splitOnCapitalization(propertyName);
        this.property = ReflectionUtils.getField(ReviewRequestDraft.class, propertyName);
    }

    @Override
    public String cannotRunAction() {
        String propertyValue = (String) ReflectionUtils.getValue(property, draft);
        if (!commitConfig.setEmptyPropertiesOnly || StringUtils.isEmpty(propertyValue)) {
            return super.cannotRunAction();
        }

        return "setEmptyPropertiesOnly is set to true and " + title + " has a value";
    }


}
