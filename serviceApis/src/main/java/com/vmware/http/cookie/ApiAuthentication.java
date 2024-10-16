package com.vmware.http.cookie;

import com.vmware.util.exception.FatalException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum detailing the cookie names if applicable and the file system locations for each cookie / token.
 * All file are assumed to be in the user's home folder.
 */
public enum ApiAuthentication {
    reviewBoard_cookie("rbsessionid", ".post-review-cookies.txt"),
    reviewBoard_token(".rb-api-token.txt"),
    jira_token(".jira-access-token.txt"),
    bugzilla_login_id("Bugzilla_login", ".bugzilla-login-id.txt"),
    bugzilla_cookie("Bugzilla_logincookie", ".bugzilla-cookies.txt"),
    jenkins_token(".jenkins-api-token.txt"),
    trello_token(".trello-api-token.txt"),
    vcd_token(".vcd-api-token.txt"),
    vcd_refresh(".vcd-refresh-token.txt"),
    gitlab_token(".gitlab-access-token.txt"),
    github_token(".github-access-token.txt"),
    none("");

    private final String cookieName;
    private final String fileName;

    private ApiAuthentication(final String fileName) {
        this(null, fileName);
    }

    private ApiAuthentication(final String cookieName, final String fileName) {
        this.cookieName = cookieName;
        this.fileName = fileName;
    }

    public String getCookieName() {
        return cookieName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayType() {
        return cookieName != null ? "cookie" : "token";
    }

    public static ApiAuthentication loadByCookieName(String cookieName) {
        for (ApiAuthentication definition : ApiAuthentication.values()) {
            if (definition.getCookieName() != null && definition.getCookieName().equals(cookieName)) {
                return definition;
            }
        }
        return null;
    }

    public static ApiAuthentication loadByName(String name) {
        try {
            return ApiAuthentication.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new FatalException("{} name one of {} values", name, Arrays.stream(ApiAuthentication.values()).map(Enum::name).collect(Collectors.joining(",")));
        }
    }
}
