package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.utils.input.InputUtils;

import java.io.IOException;

import static com.vmware.utils.StringUtils.NEW_LINE_CHAR;

public abstract class BaseReadMultiLine extends BaseCommitReadAction {

    private final boolean append;

    private final String[] historyValues;

    public BaseReadMultiLine(WorkflowConfig config, String propertyName, boolean append, String... historyValues) throws NoSuchFieldException {
        super(config, propertyName);
        this.append = append;
        this.historyValues = historyValues;
    }

    @Override
    public void process() throws IOException, IllegalAccessException {
        String propertyValue = (String) property.get(draft);
        
        if (propertyValue == null) {
            propertyValue = "";
        }
        if (!propertyValue.isEmpty()) {
            log.info("Existing value for section {}{}{}", property.getName(), NEW_LINE_CHAR, propertyValue);
        }
        String titleToDisplay = propertyValue.isEmpty() || !append ? title : "additional " + title;
        if (append) {
            propertyValue += NEW_LINE_CHAR + InputUtils.readData(titleToDisplay, false, config.maxDescriptionLength);
        } else {
            propertyValue = InputUtils.readData(titleToDisplay, false, config.maxDescriptionLength, historyValues);
        }
        property.set(draft, propertyValue);
    }
}
