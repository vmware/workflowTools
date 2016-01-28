package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.input.InputUtils;

import static com.vmware.util.StringUtils.NEW_LINE_CHAR;

public abstract class BaseReadMultiLine extends BaseCommitReadAction {

    private final boolean append;

    private final String[] historyValues;

    public BaseReadMultiLine(WorkflowConfig config, String propertyName, boolean append, String... historyValues) {
        super(config, propertyName);
        this.append = append;
        this.historyValues = historyValues;
    }

    @Override
    public void process() {
        String propertyValue;
        try {
            propertyValue = (String) property.get(draft);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }

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
        try {
            property.set(draft, propertyValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }
}
