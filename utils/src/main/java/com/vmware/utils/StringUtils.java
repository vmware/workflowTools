package com.vmware.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class StringUtils {
    public static final String NEW_LINE_CHAR = "\n";

    private static Logger log = LoggerFactory.getLogger(StringUtils.class.getName());

    public static String addToCsvValue(String existingValue, String valueToAdd) {
        if (!existingValue.isEmpty()) {
            existingValue += ",";
        }
        existingValue += valueToAdd;
        return existingValue;
    }

    public static boolean isInteger(String value) {
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
            for (String word : words) {
                if (!word.equals("\n") && word.length() + newLine.length() <= maxLengthToUse) {
                    newLine += word;
                    if (newLine.length() < maxLengthToUse) {
                        newLine += " ";
                    }
                } else {
                    if (newLine.length() > maxLineLength) {
                        log.warn("Probable Bug: line \n{}\n was greater than the max line length of {}", newLine, maxLineLength);
                    }
                    newValue += newLine + "\n";
                    newLine = word.equals("\n") ? "" : word + " ";
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

    public static boolean isNotBlank(String value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    public static String repeat(int length, String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

}
