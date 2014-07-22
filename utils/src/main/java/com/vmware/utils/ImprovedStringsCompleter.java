package com.vmware.utils;

import jline.console.completer.Completer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jline.internal.Preconditions.checkNotNull;

/**
 * Completer for a set of values.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class ImprovedStringsCompleter
        implements Completer
{
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

    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        // candidates could be null
        checkNotNull(candidates);

        if (buffer == null || buffer.isEmpty()) {
            candidates.addAll(valuesShownWhenNoBuffer);
        }
        else {
            for (String value : values.tailSet(buffer)) {
                if (bufferMatchesValue(buffer, value)) {
                    candidates.add(value);
                }
            }
        }

        if (candidates.size() == 1) {
            candidates.set(0, candidates.get(0) + delimeterText);
        }

        return candidates.isEmpty() ? -1 : 0;
    }

    private boolean bufferMatchesValue(String buffer, String value) {
        String pattenForBuffer = generatePatternForBuffer(buffer);
        Matcher valueMatcher = Pattern.compile(pattenForBuffer).matcher(value);
        return valueMatcher.find();
    }

    private String generatePatternForBuffer(String buffer) {
        String pattern = "^";
        for (char character : buffer.toCharArray()) {
            if (Character.isUpperCase(character) && pattern.length() > 1) {
                pattern += "[a-z]*" + character;
            } else {
                pattern += character;
            }
        }

        return pattern;
    }
}