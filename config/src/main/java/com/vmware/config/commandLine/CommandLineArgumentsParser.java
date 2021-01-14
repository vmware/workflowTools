package com.vmware.config.commandLine;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.util.ArrayUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed command line arguments.
 * Parsed values override workflow config values.
 */
public class CommandLineArgumentsParser {

    private Map<String, String> argumentMap = new HashMap<String, String>();

    private StringBuilder argumentsText;

    public void generateArgumentMap(final String[] args) {
        argumentsText = new StringBuilder();

        argumentMap.clear();

        for (int i = 0; i < args.length; i ++) {
            if (i > 0) {
                argumentsText.append(" ");
            }
            if (!args[i].startsWith("-")) {
                argumentsText.append(args[i]);
                continue;
            }
            String[] paramPieces = args[i].split("=");
            String paramName = paramPieces[0];
            argumentsText.append(paramName);
            String paramValue = null;

            if (paramPieces.length == 1 && args[i].endsWith("=")) {
                paramValue = "";
            } else if (paramPieces.length == 2) {
                paramValue = paramPieces[1];
            } else if (i < args.length - 1 && !args[i+1].startsWith("-")) {
                paramValue = args[++i];
            }
            if (paramValue != null) {
                argumentsText.append("=").append(paramValue.contains(" ") ? "\"" + paramValue + "\"" : paramValue);
            }

            argumentMap.put(paramName, paramValue);
        }
        // add first variable as a possible workflow value if it does not start with -
        if (args.length > 0 && !args[0].startsWith("-")) {
            argumentMap.put("--possible-workflow", args[0]);
        }
    }

    public Map<String, String> getArgumentMap() {
        return Collections.unmodifiableMap(argumentMap);
    }

    public String getArgumentsText() {
        return argumentsText.toString();
    }

    public String getMatchingArgumentKey(String... possibleMatchingValues) {
        String foundKey = null;
        for (String key : possibleMatchingValues) {
            if (argumentMap.containsKey(key)) {
                if (foundKey != null) {
                    throw new FatalException(
                            "Both {} and {} command line arguments found, only one should be set", foundKey, key);
                }
                foundKey = key;
            }
        }
        return foundKey;
    }

    public String getArgument(String defaultValue, String... possibleMatchingValues) {
        String matchingArgumentKey = this.getMatchingArgumentKey(possibleMatchingValues);
        if (matchingArgumentKey == null) {
            return defaultValue;
        }

        String argumentValue = argumentMap.get(matchingArgumentKey);
        if (argumentValue == null) {
            throw new FatalException("Command line argument {} did not specify a value", matchingArgumentKey);
        }
        return argumentValue;
    }

    @Override
    public String toString() {
        StringBuilder argumentText = new StringBuilder();
        for (String argumentKey : argumentMap.keySet()) {
            String argumentValue = argumentMap.get(argumentKey);
            if (argumentText.length() > 0) {
                argumentText.append("\n");
            }
            argumentText.append(argumentKey);
            if (argumentValue != null) {
                argumentText.append("=").append(argumentValue);
            }
        }
        return argumentText.toString();
    }
}
