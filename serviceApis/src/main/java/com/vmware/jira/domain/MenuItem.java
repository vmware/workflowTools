package com.vmware.jira.domain;

import com.vmware.util.input.InputListSelection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuItem implements InputListSelection {

    public String id;

    public String title;

    public String label;

    public String url;

    public String getBoardId() {
        if (id == null) {
            return null;
        }
        Matcher idMatcher = Pattern.compile("rapidb_lnk_(\\d+)").matcher(id);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        } else {
            return null;
        }
    }

    public boolean isRealItem() {
        return !label.equals("more…");
    }

    @Override
    public String getLabel() {
        return label;
    }
}
