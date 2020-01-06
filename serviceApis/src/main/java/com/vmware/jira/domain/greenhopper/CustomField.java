package com.vmware.jira.domain.greenhopper;

import com.vmware.util.StringUtils;

public class CustomField {

    public String statFieldId;

    public CustomFieldValue statFieldValue;

    public boolean containsValidEstimate() {
        if (statFieldValue == null || "issueCount".equals(statFieldId))  {
            return false;
        }

        return StringUtils.isNotEmpty(statFieldValue.text);
    }
}
