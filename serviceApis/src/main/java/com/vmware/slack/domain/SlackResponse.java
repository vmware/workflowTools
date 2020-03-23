package com.vmware.slack.domain;

import java.util.Map;

public class SlackResponse {
    public boolean ok;
    public String error;
    public String needed;
    public String provided;
    public String warning;
    public String deprecatedArgument;
    public Map<String, String[]> responseMetadata;

    @Override
    public String toString() {
        if (ok) {
            return "ok";
        } else if ("missing_scope".equals(error)) {
            return error + " - needed: " + needed + " provided: " + provided;
        } else if ("invalid_arguments".equals(error) && deprecatedArgument != null) {
            return error + " deprecated argument: " + deprecatedArgument;
        } else {
            return error;
        }
    }
}
