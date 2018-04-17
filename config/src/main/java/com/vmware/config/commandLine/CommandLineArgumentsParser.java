package com.vmware.config.commandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vmware.config.ConfigurableProperty;
import com.vmware.util.ArrayUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

/**
 * Parsed command line arguments.
 * Parsed values override workflow config values.
 */
public class CommandLineArgumentsParser {

    private static final String[] ADDITIONAL_ARGUMENT_NAMES = new String[] {"-c", "--config", "--possible-workflow"};

    private Map<String, String> argumentMap = new HashMap<String, String>();

    private String argumentsText;

    public void generateArgumentMap(final String[] args) {
        argumentsText = "";

        argumentMap.clear();

        for (int i = 0; i < args.length; i ++) {
            if (i > 0) {
                argumentsText += " ";
            }
            if (!args[i].startsWith("-")) {
                argumentsText += args[i];
                continue;
            }
            String[] paramPieces = args[i].split("=");
            String paramName = paramPieces[0];
            argumentsText += paramName;
            String paramValue = null;

            if (paramPieces.length == 1 && args[i].endsWith("=")) {
                paramValue = "";
            } else if (paramPieces.length == 2) {
                paramValue = paramPieces[1];
            } else if (i < args.length - 1 && !args[i+1].startsWith("-")) {
                paramValue = args[++i];
            }
            if (paramValue != null) {
                argumentsText += "=" + (paramValue.contains(" ") ? "\"" + paramValue + "\"" : paramValue);
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
        return argumentsText;
    }

    public boolean containsArgument(String... possibleMatchingValues) {
        return getMatchingArgumentKey(possibleMatchingValues) != null;
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

    public String getExpectedArgument(String... possibleMatchingValues) {
        String NO_DEFAULT = null;

        String argValue = getArgument(NO_DEFAULT, possibleMatchingValues);

        if (argValue == null) {
            throw new FatalException("Expected to find match for config names " + Arrays.toString(possibleMatchingValues));
        }
        return argValue;
    }

    public void checkForUnrecognizedArguments(List<ConfigurableProperty> validProperties) {
        List<UnrecognizedCommandLineArgument> unrecognizedArguments = new ArrayList<>();

        for (String argument : argumentMap.keySet()) {
            if (ArrayUtils.contains(ADDITIONAL_ARGUMENT_NAMES, argument)) {
                continue;
            }
            if (argument.startsWith("--J")) {
                continue;
            }
            UnrecognizedCommandLineArgument potentiallyUnrecognizedArgument = new UnrecognizedCommandLineArgument(argument);
            for (ConfigurableProperty validProperty : validProperties) {
                List<String> validMatches = StringUtils.splitAndTrim(validProperty.commandLine(), ",");
                if (validMatches.contains(argument)) {
                    potentiallyUnrecognizedArgument = null;
                    break;
                }
                int numberOfCharactersToCheck = argument.startsWith("--") ? 3 : 2;
                String argumentFragmentToPartialMatch = argument.substring(0,numberOfCharactersToCheck);
                for (String validMatch : validMatches) {
                    if (validMatch.startsWith(argumentFragmentToPartialMatch)) {
                        potentiallyUnrecognizedArgument.addPossibleArgument(validMatch, validProperty);
                    }
                }

            }
            if (potentiallyUnrecognizedArgument != null) {
                unrecognizedArguments.add(potentiallyUnrecognizedArgument);
            }
        }
        if (!unrecognizedArguments.isEmpty()) {
            String errorMessage = "Following arguments were unrecognized\n";
            for (UnrecognizedCommandLineArgument unrecognizedArgument : unrecognizedArguments) {
                errorMessage += "\n" + unrecognizedArgument.toString() + "\n";
            }
            throw new FatalException(errorMessage);
        }
    }

    @Override
    public String toString() {
        String argumentText = "";
        for (String argumentKey : argumentMap.keySet()) {
            String argumentValue = argumentMap.get(argumentKey);
            if (!argumentText.isEmpty()) {
                argumentText += "\n";
            }
            argumentText += argumentKey;
            if (argumentValue != null) {
                argumentText += "=" + argumentValue;
            }
        }
        return argumentText;
    }
}
