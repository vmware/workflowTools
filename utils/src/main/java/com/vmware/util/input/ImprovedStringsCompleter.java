package com.vmware.util.input;

import static jline.internal.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import jline.console.completer.Completer;

/**
 * Based on existing StringsCompleter in JLine2.
 * Adds support for not showing some values when no text entered.
 */
public class ImprovedStringsCompleter implements Completer {

    protected boolean caseInsensitiveMatching;
    protected final SortedSet<String> values = new TreeSet<String>();

    protected final SortedSet<String> valuesShownWhenNoBuffer = new TreeSet<String>();

    private String delimeterText = " ";

    public ImprovedStringsCompleter() {
        // empty
    }

    public ImprovedStringsCompleter(final Collection<String> values) {
        checkNotNull(values);
        for (String value : values) {
            addValue(value);
        }
    }

    public ImprovedStringsCompleter(final String... values) {
        this(Arrays.asList(values));
    }

    public Collection<String> getValues() {
        return values;
    }

    public void addValue(String value) {
        if (!value.startsWith("!")) {
            this.values.add(value);
            this.valuesShownWhenNoBuffer.add(value);
        } else {
            String visibleValue = value.substring(1);
            this.values.add(visibleValue);
        }
    }

    public void setDelimeterText(String delimeterText) {
        this.delimeterText = delimeterText;
    }

    public void setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        this.caseInsensitiveMatching = caseInsensitiveMatching;
    }

    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        // candidates could be null
        checkNotNull(candidates);

        if (buffer == null || buffer.isEmpty()) {
            candidates.addAll(valuesShownWhenNoBuffer);
        } else {
            for (String value : values) {
                if (bufferMatchesValue(buffer, value)) {
                    addMatchingValueToCandidates(candidates, value);
                }
            }
        }

        if (candidates.size() == 1) {
            candidates.set(0, candidates.get(0) + delimeterText);
        }

        return candidates.isEmpty() ? -1 : 0;
    }

    protected void addMatchingValueToCandidates(List<CharSequence> candidates, String value) {
        candidates.add(value);
    }

    protected boolean bufferMatchesValue(String buffer, String value) {
        if (caseInsensitiveMatching) {
            return value.toLowerCase().startsWith(buffer.toLowerCase());
        }
        Pattern caseSensitivePattern = createCaseSensitivePattern(buffer);
        return caseSensitivePattern.matcher(value).find();

    }

    protected Pattern createCaseSensitivePattern(String buffer) {
        String pattern = "^";
        for (char character : buffer.toCharArray()) {
            if (Character.isUpperCase(character) && pattern.length() > 1) {
                pattern += "[a-z]*" + character;
            } else {
                pattern += character;
            }
        }
        return Pattern.compile(pattern);
    }
}