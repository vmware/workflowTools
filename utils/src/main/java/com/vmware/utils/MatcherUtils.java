package com.vmware.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatcherUtils {

    public static String singleMatch(String text, String pattern, int flags) {
        Matcher matcher = Pattern.compile(pattern, flags).matcher(text);
        return getMatchedValue(matcher);
    }

    public static String singleMatch(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return getMatchedValue(matcher);
    }

    private static String getMatchedValue(Matcher matcher) {
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
