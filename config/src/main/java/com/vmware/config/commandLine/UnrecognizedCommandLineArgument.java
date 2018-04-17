/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.config.commandLine;

import java.util.HashMap;
import java.util.Map;

import com.vmware.config.ConfigurableProperty;

public class UnrecognizedCommandLineArgument {
    private String missingArgumentValue;

    private Map<String, ConfigurableProperty> possibleArguments = new HashMap<String, ConfigurableProperty>();

    public UnrecognizedCommandLineArgument(String missingArgumentValue) {
        this.missingArgumentValue = missingArgumentValue;
    }

    public void addPossibleArgument(String possibleArgument, ConfigurableProperty configurableProperty) {
        possibleArguments.put(possibleArgument, configurableProperty);
    }

    @Override
    public String toString() {
        String text = "Unrecognized argument: " + missingArgumentValue;
        for (String possibleArgument : possibleArguments.keySet()) {
            text += "\nDid you mean " + possibleArgument + ": " + possibleArguments.get(possibleArgument).help();
        }
        return text;
    }
}
