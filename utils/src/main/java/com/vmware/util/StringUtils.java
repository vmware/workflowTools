package com.vmware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.vmware.util.exception.FatalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtils {
    public static final String NEW_LINE_CHAR = "\n";

    private static Logger log = LoggerFactory.getLogger(StringUtils.class.getName());

    public static void throwFatalExceptionIfBlank(String value, String propertyName) {
        if (StringUtils.isEmpty(value)) {
            throw new FatalException("{} cannot be null", propertyName);
        }
    }

    public static boolean textStartsWithValue(String text, String... valuesToCheck) {
        return Arrays.stream(valuesToCheck).anyMatch(text::startsWith);
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
        if (isEmpty(value)) {
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
        log.debug("Truncating string \n{}\nto\n{}", value, newValue);
        return newValue;
    }

    public static String addNewLinesIfNeeded(String value, int maxLineLength, int labelLength) {
        String newValue = "";
        String[] lines = value.split("[\r\n]");
        for (int i = 0; i < lines.length; i++) {
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
            argument = argument.replace("$", "\\$");
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
        for (String value : values) {
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
        return value != null && isNotEmpty(value.trim());
    }


    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isEmpty(String value) {
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

    public static int occurenceCount(String text, String searchText) {
        int index = text.indexOf(searchText);
        int counter = 0;
        while (index != -1) {
            index = text.indexOf(searchText, index + 1);
            counter++;
        }
        return counter;
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
            StringBuilder displayText = new StringBuilder();
            for (Object key : values.keySet()) {
                Object displayValue = convertObjectToString(values.get(key));
                displayText.append("\n").append(key).append(": ").append(displayValue);
            }
            return displayText.toString();
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

    public static String pluralize(long value, String description) {
        if (value > 1) {
            return value + " " + description + "s";
        } else {
            return value + " " + description;
        }
    }

    public static String[] splitOnlyOnce(String value, String delimeter) {
        if (value == null) {
            return null;
        }
        int firstIndex = value.indexOf(delimeter);
        if (firstIndex == -1) {
            return new String[] { value };
        } else {
            return new String[] {value.substring(0, firstIndex), value.substring(firstIndex + 1)};
        }
    }

    public static List<String> splitAndTrim(String value, String delimeter) {
        if (value == null) {
            return Collections.emptyList();
        }
        String[] spltValues = value.split(delimeter);
        return Arrays.stream(spltValues).filter(StringUtils::isNotEmpty).map(String::trim).collect(Collectors.toList());
    }

    public static String unescapeJavaString(String st) {
        StringBuilder sb = new StringBuilder(st.length());

        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == st.length() - 1) ? '\\' : st
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                            && st.charAt(i + 1) <= '7') {
                        code += st.charAt(i + 1);
                        i++;
                        if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                                && st.charAt(i + 1) <= '7') {
                            code += st.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                case '\\':
                    ch = '\\';
                    break;
                case 'b':
                    ch = '\b';
                    break;
                case 'f':
                    ch = '\f';
                    break;
                case 'n':
                    ch = '\n';
                    break;
                case 'r':
                    ch = '\r';
                    break;
                case 't':
                    ch = '\t';
                    break;
                case '\"':
                    ch = '\"';
                    break;
                case '\'':
                    ch = '\'';
                    break;
                // Hex Unicode: u????
                case 'u':
                    if (i >= st.length() - 5) {
                        ch = 'u';
                        break;
                    }
                    int code = Integer.parseInt(
                            "" + st.charAt(i + 2) + st.charAt(i + 3)
                                    + st.charAt(i + 4) + st.charAt(i + 5), 16);
                    sb.append(Character.toChars(code));
                    i += 5;
                    continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }

}
