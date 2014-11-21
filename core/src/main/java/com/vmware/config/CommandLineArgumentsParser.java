package com.vmware.config;

import com.vmware.utils.ArrayUtils;
import com.vmware.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed command line arguments.
 * Parsed values override workflow config values.
 */
public class CommandLineArgumentsParser {

    private static final String[] ADDITIONAL_ARGUMENT_NAMES = new String[] {"-c", "--config", "--possible-workflow"};

    private Map<String, String> argumentMap = new HashMap<String, String>();

    private String argumentsText;

    public void generateArgumentMap(final String[] args) {
        argumentsText = StringUtils.join(Arrays.asList(args), " ");

        argumentMap.clear();

        for (int i = 0; i < args.length; i ++) {
            if (!args[i].startsWith("-")) {
                continue;
            }
            String[] paramPieces = args[i].split("=");
            String paramName = paramPieces[0];
            String paramValue = null;

            if (paramPieces.length == 2) {
                paramValue = paramPieces[1];
            } else if (i < args.length - 1 && !args[i+1].startsWith("-")) {
                paramValue = args[++i];
            }

            argumentMap.put(paramName, paramValue);
        }
        // add first variable as a possible workflow value if it does not start with -
        if (args.length > 0 && !args[0].startsWith("-")) {
            argumentMap.put("--possible-workflow", args[0]);
        }
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
                    throw new IllegalArgumentException(
                            String.format("Both %s and %s command line arguments found, only one should be set", foundKey, key));
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
            throw new IllegalArgumentException("Command line argument " + matchingArgumentKey + " did not specify a value");
        }
        return argumentValue;
    }

    public String getExpectedArgument(String... possibleMatchingValues) {
        String NO_DEFAULT = null;

        String argValue = getArgument(NO_DEFAULT, possibleMatchingValues);

        if (argValue == null) {
            throw new IllegalArgumentException("Expected to find match for config names " + Arrays.toString(possibleMatchingValues));
        }
        return argValue;
    }

    public List<UnrecognizedCommandLineArgument> findUnrecognizedArguments(List<ConfigurableProperty> validProperties) {
        List<UnrecognizedCommandLineArgument> unrecognizedArguments = new ArrayList<UnrecognizedCommandLineArgument>();

        for (String argument : argumentMap.keySet()) {
            if (ArrayUtils.contains(ADDITIONAL_ARGUMENT_NAMES, argument)) {
                continue;
            }
            UnrecognizedCommandLineArgument potentiallyUnrecognizedArgument = new UnrecognizedCommandLineArgument(argument);
            for (ConfigurableProperty validProperty : validProperties) {
                String[] validMatches = validProperty.commandLine().split(",");
                if (ArrayUtils.contains(validMatches,argument)) {
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
        return unrecognizedArguments;
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
