package com.vmware.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

public class StringUtils {
    public static final String NEW_LINE_CHAR = "\n";

    private static Logger log = LoggerFactory.getLogger(StringUtils.class.getName());

    public static boolean textStartsWithValue(String text, String... valuesToCheck) {
        for (String value : valuesToCheck) {
            if (text.startsWith(value)) {
                return true;
            }
        }
        return false;
    }

    public static String appendCsvValue(String existingValue, String valueToAdd) {
        return appendWithDelimiter(existingValue, valueToAdd, ",");
    }

    public static String appendWithDelimiter(String existingValue, Collection valuesToAdd, String delimiter) {
        for (Object valueToAdd : valuesToAdd) {
            existingValue = appendWithDelimiter(existingValue, valueToAdd.toString(), delimiter);
        }
        return existingValue;
    }

    public static String appendWithDelimiter(String existingValue, String valueToAdd, String delimiter) {
        if (!existingValue.isEmpty()) {
            existingValue += delimiter;
        }
        existingValue += valueToAdd;
        return existingValue;
    }

    public static boolean isInteger(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String truncateStringIfNeeded(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        String newValue = value.substring(0, maxLength -3) + "...";
        log.info("Truncating string \n{}\nto\n{}", value, newValue);
        return newValue;
    }

    public static String addNewLinesIfNeeded(String value, int maxLineLength, int labelLength) {
        String newValue = "";
        String[] lines = value.split("[\r\n]");
        for (int i = 0; i < lines.length; i ++) {
            String line = lines[i];
            int maxLengthToUse = newValue.isEmpty() && labelLength > 0 ? maxLineLength - labelLength: maxLineLength;
            String newLine = "";
            String[] words = line.split(" ");
            boolean onFirstWord = true;
            for (String word : words) {
                int lengthOfSpaceBetweenWords = onFirstWord ? 0 : 1;
                if (!word.equals("\n") && (word.length() + lengthOfSpaceBetweenWords) + newLine.length() <= maxLengthToUse) {
                    if (onFirstWord) {
                        onFirstWord = false;
                    } else {
                        newLine += " ";
                    }
                    newLine += word;
                } else {
                    if (newLine.length() > maxLineLength) {
                        log.warn("Probable Bug: line \n{}\n was greater than the max line length of {}", newLine, maxLineLength);
                    }
                    newValue += newLine + "\n";
                    newLine = word.equals("\n") ? "" : word;
                    maxLengthToUse = maxLineLength;
                }
            }
            newValue += newLine;
            if (i < lines.length -1) {
                newValue += "\n";
            }
        }
        return newValue;
    }

    public static String addArgumentsToValue(String value, String... arguments) {
        if (arguments.length == 0) {
            return value;
        }
        for (String argument : arguments) {
            if (argument == null) {
                argument = "";
            }
            value = value.replaceFirst("\\{\\}", argument);
        }
        return value;
    }

    public static String join(Collection<String> values) {
        return join(values, ",");
    }

    public static String join(Collection<String> values, String delimeter) {
        if (values == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values){
            if (builder.length() != 0) {
                builder.append(delimeter);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    public static String splitOnCapitalization(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String splitText = "";

        for (char character : value.toCharArray()) {
            if (splitText.isEmpty()) {
                splitText += Character.toUpperCase(character);
                continue;
            }

            if (Character.isUpperCase(character)) {
                splitText += " ";
            }

            splitText += character;
        }

        return splitText;
    }

    public static boolean equals(String arg1, String arg2) {
        return arg1 == null ? arg2 == null : arg1.equals(arg2);
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    public static int indexOrNthOccurence(String text, String searchText, int count) {
        int index = text.indexOf(searchText);
        int counter = 1;
        while (index != -1 && counter < count) {
            index = text.indexOf(searchText, index + 1);
            counter++;
        }
        return index;
    }

    public static String repeat(int length, String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    public static String convertObjectToString(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof int[]) {
            return Arrays.toString((int[]) value);
        } else if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        } else if (value instanceof Map) {
            Map values = (Map) value;
            String displayText = "";
            for (Object key : values.keySet()) {
                Object displayValue = convertObjectToString(values.get(key));
                displayText += "\n" + key + ": " + displayValue;
            }
            return displayText;
        } else {
            return value.toString();
        }
    }

    public static String stripLinesStartingWith(String text, String... textsToCheckFor) {
        String paddedText = "\n" + text + "\n"; // pad with new lines so that searches work for start and end lines
        for (String textToCheckFor : textsToCheckFor) {
            paddedText = paddedText.replaceAll("\n" + Pattern.quote(textToCheckFor) + ".+\n", "\n");
        }
        return paddedText.substring(1, paddedText.length() - 1);
    }

}
