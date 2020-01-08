package com.vmware.http.cookie;

/**
 * Enum detailing the cookie names if applicable and the file system locations for each cookie / token.
 * All file are assumed to be in the user's home folder.
 */
public enum ApiAuthentication {
    reviewBoard("rbsessionid", ".post-review-cookies.txt"),
    jira("seraph.rememberme.cookie", ".jira-cookies.txt"),
    bugzilla_login_id("Bugzilla_login", ".bugzilla-login-id.txt"),
    bugzilla_cookie("Bugzilla_logincookie", ".bugzilla-cookies.txt"),
    jenkins(".jenkins-api-token.txt"),
    trello(".trello-api-token.txt"),
    vcd(".vcd-api-token.txt"),
    gitlab(".gitlab-access-token.txt"),
    none("");

    private String cookieName;
    private String fileName;

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

    public static ApiAuthentication loadByName(String cookieName) {
        for (ApiAuthentication definition : ApiAuthentication.values()) {
            if (definition.getCookieName() != null && definition.getCookieName().equals(cookieName)) {
                return definition;
            }
        }
        return null;
    }
}
