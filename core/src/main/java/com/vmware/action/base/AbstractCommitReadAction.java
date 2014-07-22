package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

public abstract class AbstractCommitReadAction extends AbstractCommitAction {

    protected String title;

    protected Field property;

    public AbstractCommitReadAction(WorkflowConfig config, String propertyName) throws NoSuchFieldException {
        super(config);
        this.title = StringUtils.splitOnCapitalization(propertyName);
        this.property = ReviewRequestDraft.class.getField(propertyName);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        String propertyValue = (String) property.get(draft);
        if (!config.setEmptyPropertiesOnly || StringUtils.isBlank(propertyValue)) {
            return true;
        }

        log.info("Skipping action {} as setEmptyPropertiesOnly is set to true and {} has a value",
                this.getClass().getSimpleName(), title);
        return false;
    }


}
