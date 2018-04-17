package com.vmware.reviewboard.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.SerializedName;

public class ReviewUser {

    private static final Pattern userPattern = Pattern.compile("(\\w+)\\s+\\((.+?)\\s+(.+?)\\)");

    private static final Logger log = LoggerFactory.getLogger(ReviewUser.class);
    public String username;

    @SerializedName("first_name")
    public String firstName;

    @SerializedName("last_name")
    public String lastName;

    public ReviewUser() {}

    public ReviewUser(String username, String firstName, String lastName) {
        this();
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static ReviewUser fromText(String sourceText) {
        Matcher userMatcher = userPattern.matcher(sourceText);
        if (!userMatcher.find()) {
            log.debug("Failed to match user value {} with pattern {}", sourceText, userPattern.pattern());
            return null;
        }
        return new ReviewUser(userMatcher.group(1), userMatcher.group(2), userMatcher.group(3));
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public String toString() {
        return username + " (" + fullName() + ")";
    }
}
