package com.vmware.utils.input;

import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class InputUtils {

    private static Logger log = LoggerFactory.getLogger(InputUtils.class);

    private static final String MAX_LENGTH_INDICATOR = "*";

    public static Integer readSelection(Collection<String> choices, String title) throws IOException {
        return readSelection(choices.toArray(new String[choices.size()]), title);
    }

    public static Integer readSelection(InputListSelection[] choices, String title) throws IOException {
        String[] choiceTexts = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            choiceTexts[i] = choices[i].getLabel();
        }
        return readSelection(choiceTexts, title);
    }

    public static Integer readSelection(String[] choices, String title) throws IOException {
        if (choices == null || choices.length == 0) {
            throw new IllegalArgumentException("No " + title + " to select from");
        }

        Padder padder = new Padder(title);
        padder.infoTitle();
        int counter = 1;
        for (String choice : choices) {
            log.info("[{}] {}", counter++, choice);
        }
        padder.infoTitle();

        Integer selection = null;
        while (selection == null) {
            String selectionValue = readValue("Enter selection");
            if (!StringUtils.isInteger(selectionValue)) {
                log.info("Please enter a valid number");
            } else {
                selection = Integer.parseInt(selectionValue);
                if (selection < 1 || selection > choices.length) {
                    log.info("Please enter a number between {} and {}", 1, choices.length);
                    selection = null;
                }
            }
        }
        return selection - 1;
    }

    public static String readValueUntilNotBlank(String label, Collection<String> autoCompleteOptions) throws IOException {
        return readValueUntilNotBlank(label, autoCompleteOptions.toArray(new String[autoCompleteOptions.size()]));
    }

    public static String readValueUntilNotBlank(String label, String... autoCompleteOptions) throws IOException {
        boolean valueEntered = false;
        String value = null;
        while (!valueEntered) {
            value = readValue(label, autoCompleteOptions);
            if (StringUtils.isNotBlank(value)) {
                valueEntered = true;
            } else {
                log.error("No value entered!");
            }
        }
        return value;
    }

    public static int readValueUntilValidInt(String label) throws IOException {
        boolean valueEntered = false;
        int value = 0;
        while (!valueEntered) {
            String text = readValueUntilNotBlank(label);
            if (StringUtils.isInteger(text)) {
                valueEntered = true;
                value = Integer.parseInt(text);
            } else {
                log.error("{} is not an integer!", text);
            }
        }
        return value;
    }

    public static String readValue(String label, Completer completer, List<String> historyValues) throws IOException {
        return readSingleLine(label, null, null, historyValues.toArray(new String[historyValues.size()]), completer);
    }

    public static String readValue(String label, Collection<String> autocompleteOptions) throws IOException {
        return readSingleLine(label, null, null, null, autocompleteOptions.toArray(new String[autocompleteOptions.size()]));
    }

    public static String readValue(String label, String... autocompleteOptions) throws IOException {
        return readSingleLine(label, null, null, null, autocompleteOptions);
    }

    public static String readPassword(String label) throws IOException {
        return readSingleLine(label, null, '*', null);
    }

    public static String readData(String label, boolean singleLine, Integer maxLength, String... historyValues) throws IOException {
        String data;
        if (singleLine) {
            data = readSingleLine(label, maxLength, null, historyValues);
        } else {
            String fullText = readMultipleLines(label, maxLength, historyValues);
            data = maxLength != null ? StringUtils.addNewLinesIfNeeded(fullText, maxLength, 0) : fullText;
        }
        return data;
    }

    private static String readMultipleLines(String label, Integer maxLength, String... historyValues) throws IOException {
        String displayLabel = String.format("%s (Type / then press Enter to finish input): ", label);
        if (maxLength != null) {
            int paddingLength = maxLength - MAX_LENGTH_INDICATOR.length() - displayLabel.length();
            System.err.println("\n" + displayLabel + StringUtils.repeat(paddingLength, " ") + MAX_LENGTH_INDICATOR);
        }

        boolean nextLine = true;
        String data = "";
        ConsoleReader consoleReader = new ConsoleReader();
        consoleReader.setExpandEvents(false);
        addHistoryValues(consoleReader, historyValues);
        int numberOfTrailingCharsToDiscard = 0;
        while (nextLine) {
            String line = consoleReader.readLine();
            int usableLineLength = determineLineLength(line);

            // if they are not the same then an end of text character was entered
            if (usableLineLength < line.length()) {
                data += line;
                numberOfTrailingCharsToDiscard = line.length() - usableLineLength;
                nextLine = false;
            } else {
                data += line + "\n";
            }
        }
        data = data.substring(0, data.length() - numberOfTrailingCharsToDiscard);
        return data;
    }

    private static int determineLineLength(String line) {
        if (line.length() >= 1 && line.endsWith("/")) {
            return line.length() - 1;
        } else {
            return line.length();
        }
    }

    private static String readSingleLine(String label, Integer maxLength, Character maskCharacter, String[] historyValues
            , String... autocompleteOptions) throws IOException {
        Completer completer = createCompleterFromOptions(autocompleteOptions);
        return readSingleLine(label, maxLength, maskCharacter, historyValues, completer);
    }

    private static String readSingleLine(String label, Integer maxLength, Character maskCharacter, String[] historyValues, Completer completer) throws IOException {
        ConsoleReader consoleReader = new ConsoleReader();
        consoleReader.setExpandEvents(false);
        if (completer != null) {
            consoleReader.addCompleter(completer);
        }

        String prompt = String.format("%s: ", label);
        if (maxLength != null) {
            System.err.println(
                    StringUtils.repeat(maxLength + prompt.length() - MAX_LENGTH_INDICATOR.length(), " ")
                            + MAX_LENGTH_INDICATOR);
        }

        addHistoryValues(consoleReader, historyValues);

        String data = consoleReader.readLine(prompt, maskCharacter);
        if (maxLength != null && data.length() > maxLength) {
            log.error("Re-enter line. Line length of {} exceeded max of {}", data.length(), maxLength);
            data = readSingleLine(label, maxLength, maskCharacter, historyValues, completer);
        }
        return data;
    }

    private static Completer createCompleterFromOptions(String[] autocompleteOptions) {
        if (autocompleteOptions == null || autocompleteOptions.length == 0) {
            return null;
        }
        ImprovedStringsCompleter completer = new ImprovedStringsCompleter(autocompleteOptions);
        completer.setDelimeterText("");
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(new CommaArgumentDelimeter(), completer);
        argumentCompleter.setStrict(false);
        return argumentCompleter;
    }

    private static void addHistoryValues(ConsoleReader reader, String[] values) {
        if (values == null) {
            return;
        }

        // add in reverse so that order is as user expects
        for (int i = values.length -1; i >= 0; i --) {
            reader.getHistory().add(values[i]);
        }
    }

}
