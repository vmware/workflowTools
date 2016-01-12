package com.vmware.jira.domain.greenhopper;

import com.vmware.util.StringUtils;

public class CustomField {

    public String statFieldId;

    public CustomFieldValue statFieldValue;

    public boolean containsValue() {
        if (statFieldValue == null) {
            return false;
        }

        return StringUtils.isNotBlank(statFieldValue.text);
    }
}
