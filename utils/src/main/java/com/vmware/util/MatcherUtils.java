package com.vmware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vmware.util.exception.FatalException;

public class MatcherUtils {

    public static String singleMatch(String text, String pattern, int flags) {
        Matcher matcher = Pattern.compile(pattern, flags).matcher(text);
        return getMatchedValue(matcher);
    }

    public static List<String> allMatches(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    public static String singleMatch(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return getMatchedValue(matcher);
    }

    public static String singleMatchExpected(String text, String pattern) {
        String value = singleMatch(text, pattern);
        if (value == null) {
            throw new FatalException("Pattern {} not matched in text {}", pattern, text);
        }
        return value;
    }

    private static String getMatchedValue(Matcher matcher) {
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        String bugzillaUrl = "https://bugzilla.eng.vmware.com/";
        System.out.println(MatcherUtils.singleMatch("https://bugzilla.eng.vmware.com/show_bug.cgi?id=1567574\n" +
                "Using for testing creating of matching bugs in Jira", bugzillaUrl + "/*show_bug\\.cgi\\?id=(\\d+)"));
    }
}
