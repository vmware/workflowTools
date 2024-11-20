package com.vmware.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.crypto.EncryptedPrivateKeyInfo;

import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtils {
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
    public static final String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";

    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String BEGIN_ENCRYPTED_PRIVATE_KEY = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
    public static final String END_ENCRYPTED_PRIVATE_KEY = "-----END ENCRYPTED PRIVATE KEY-----";

    private static Logger log = LoggerFactory.getLogger(StringUtils.class.getName());

    public static void throwFatalExceptionIfBlank(String value, String propertyName) {
        if (StringUtils.isEmpty(value)) {
            throw new FatalException("{} cannot be null", propertyName);
        }
    }

    public static ToStringSupplier exceptionAsString(Throwable t) {
        return ToStringSupplier.toString(() -> {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            t.printStackTrace(writer);
            return stringWriter.toString();
        });
    }

    public static String findStringWithStartAndEnd(String text, String start, String end) {
        if (!text.contains(start)) {
            throw new FatalException("Could not find {} in \n{}", start, text);
        }
        if (!text.contains(end)) {
            throw new FatalException("Could not find {} in \n{}", end, start);
        }
        int startIndex = text.indexOf(start);
        int endIndex = text.indexOf(end);

        return text.substring(startIndex, endIndex) + end;
    }

    public static String findStringBetween(String text, String start, String end) {
        String textWithStartAndEnd = findStringWithStartAndEnd(text, start, end);
        return textWithStartAndEnd.substring(start.length(), textWithStartAndEnd.lastIndexOf(end));
    }

    public static boolean startsWith(String text, String... valuesToCheck) {
        return Arrays.stream(valuesToCheck).anyMatch(text::startsWith);
    }

    public static String appendCsvValue(String existingValue, String valueToAdd) {
        return appendWithDelimiter(existingValue, valueToAdd, ",");
    }

    public static String appendWithDelimiter(String existingValue, Collection valuesToAdd, String delimiter) {
        return existingValue + valuesToAdd.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }

    public static String appendWithDelimiter(String existingValue, String valueToAdd, String delimiter) {
        if (!existingValue.isEmpty()) {
            existingValue += delimiter;
        }
        existingValue += valueToAdd;
        return existingValue;
    }

    public static String urlEncode(String value) {
        if (isEmpty(value)) {
            return value;
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeIOException(e);
        }
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

    public static boolean isLong(String value) {
        if (isEmpty(value)) {
            return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String truncateStringIfNeeded(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
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

    public static String addArgumentsToValue(String value, Object... arguments) {
        for (Object argument : arguments) {
            if (argument == null) {
                argument = "";
            }
            String argValue = String.valueOf(argument).replace("$", "\\$");
            value = value.replaceFirst("\\{\\}", argValue);
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

    public static String convertToDbName(String value) {
        StringBuilder dbName = new StringBuilder("");
        for (char character : value.toCharArray()) {
            if (dbName.length() > 0 && Character.isUpperCase(character)) {
                dbName.append("_");
            }
            dbName.append(Character.toUpperCase(character));
        }
        return dbName.toString();
    }

    public static String convertFromDbName(String value) {
        StringBuilder name = new StringBuilder("");
        boolean uppercaseNextLetter = false;
        for (char character : value.toCharArray()) {
            if (character == '_') {
                uppercaseNextLetter = true;
            } else if (uppercaseNextLetter) {
                name.append(Character.toUpperCase(character));
                uppercaseNextLetter = false;
            } else {
                name.append(Character.toLowerCase(character));
            }
        }
        return name.toString();
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

    public static String substringBefore(String value, String valueToCheckFor) {
        if (value == null || !value.contains(valueToCheckFor)) {
            return value;
        }
        return value.substring(0, value.indexOf(valueToCheckFor));
    }

    public static String substringAfter(String value, String valueToCheckFor) {
        if (value == null) {
            return null;
        }
        if (!value.contains(valueToCheckFor)) {
            return value;
        }
        return value.substring(value.indexOf(valueToCheckFor) + valueToCheckFor.length());
    }

    public static String substringAfterLast(String value, String valueToCheckFor) {
        if (value == null) {
            return null;
        }
        if (!value.contains(valueToCheckFor)) {
            return value;
        }
        return value.substring(value.lastIndexOf(valueToCheckFor) + valueToCheckFor.length());
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

    public static String humanReadableSize(String bytes) {
        if (!StringUtils.isLong(bytes)) {
            log.debug("Bytes size {} is not a long value", bytes);
            return "";
        }
        return humanReadableSize(Long.parseLong(bytes));
    }

    public static String humanReadableSize(long bytesValue) {
        long absB = bytesValue == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytesValue);
        if (absB < 1024) {
            return bytesValue + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytesValue);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
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
        return value + " " + pluralizeDescription(value, description);
    }

    public static String pluralizeDescription(long value, String description) {
        if (value == 1) {
            return description;
        } else {
            return description + "s";
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

    public static String trim(String value) {
        return value != null ? value.trim() : null;
    }

    public static String replaceLineBreakWithHtmlBrTag(String text) {
        if (text == null) {
            return text;
        }
        return text.replace(System.lineSeparator(), "<br/>");
    }

    public static List<String> splitAndTrim(String value, String delimeter) {
        if (value == null) {
            return Collections.emptyList();
        }
        String[] spltValues = value.split(delimeter);
        return Arrays.stream(spltValues).filter(StringUtils::isNotEmpty).map(String::trim).collect(Collectors.toList());
    }

    public static String convertToPem(final Certificate certificate) throws CertificateEncodingException {
        return convertToPem(certificate.getEncoded(), BEGIN_CERT, END_CERT);
    }

    public static String convertToPem(final Key key) {
        return convertToPem(key.getEncoded(), BEGIN_PRIVATE_KEY, END_PRIVATE_KEY);
    }

    public static String convertToPem(final EncryptedPrivateKeyInfo key) {
        try {
            return convertToPem(key.getEncoded(), BEGIN_ENCRYPTED_PRIVATE_KEY, END_ENCRYPTED_PRIVATE_KEY);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
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

    private static String convertToPem(byte[] value, String header, String footer) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());

        final String encodedCertText = new String(encoder.encode(value));
        return header + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + footer;
    }

    public static class ToStringSupplier implements Supplier<String> {

        private Supplier<String> stringSource;

        private ToStringSupplier(Supplier<String> stringSource) {
            this.stringSource = stringSource;
        }

        public static ToStringSupplier toString(Supplier<String> supplier) {
            return new ToStringSupplier(supplier);
        }

        @Override
        public String get() {
            return stringSource.get();
        }

        @Override
        public String toString() {
            return stringSource.get();
        }
    }

}
