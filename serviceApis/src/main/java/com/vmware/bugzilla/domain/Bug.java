package com.vmware.bugzilla.domain;

import com.vmware.IssueInfo;
import com.vmware.utils.MatcherUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a bug in Bugzilla.
 */
public class Bug implements IssueInfo {

    public static final String TRACKING_ISSUE_TEXT = "Tracking this bug in Jira with issue ";

    private String key;

    private String webUrl;

    private String summary;

    private String description;

    private boolean notFound;

    private List<BugComment> comments;

    public Bug(String key) {
        this.key = key;
        this.notFound = true;
    }

    public Bug(Map values) {
        this.key =  String.valueOf(values.get("bug_id"));
        this.webUrl = String.valueOf(values.get("web_url"));
        this.summary = (String) values.get("short_desc");
        this.description = (String) values.get("description");
        this.comments = parseComments((Object[]) values.get("comments"));
        this.notFound = false;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getWebUrl() {
        return webUrl;
    }

    @Override
    public boolean isNotFound() {
        return notFound;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof String) {
            String key = (String) o;
            return key.equals(this.key);
        }

        if (o == null || getClass() != o.getClass()) return false;

        Bug bug = (Bug) o;

        if (key != null ? !key.equals(bug.key) : bug.key != null) return false;

        return true;
    }

    public String getTrackingIssueKey() {
        String trackingKeyValue = null;
        for (BugComment comment : comments) {
            String matchingKey = MatcherUtils.singleMatch(comment.getComment(), TRACKING_ISSUE_TEXT + ".+?(\\w+-\\d+)");
            if (matchingKey != null) {
                trackingKeyValue = matchingKey;
            }
        }
        return trackingKeyValue;
    }

    public boolean containsComment(String commentText) {
        for (BugComment comment : comments) {
            if (commentText.equalsIgnoreCase(comment.getComment())) {
                return true;
            }
        }
        return false;
    }

    private List<BugComment> parseComments(Object[] commentObjects) {
        if (commentObjects == null || commentObjects.length == 0) {
            return Collections.EMPTY_LIST;
        }
        List<BugComment> comments = new ArrayList<>();
        for (Object commentObject : commentObjects) {
            comments.add(new BugComment((Map) commentObject));
        }
        return comments;
    }

}
