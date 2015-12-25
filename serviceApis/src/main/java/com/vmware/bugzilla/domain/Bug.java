package com.vmware.bugzilla.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.IssueInfo;
import com.vmware.rest.json.StringEnumMapper;
import com.vmware.utils.IOUtils;
import com.vmware.utils.MatcherUtils;
import com.vmware.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.vmware.rest.json.StringEnumMapper.findByValue;

/**
 * Represents a bug in Bugzilla.
 */
public class Bug implements IssueInfo {

    public static final String TRACKING_ISSUE_TEXT = "Tracking this bug in Jira with issue ";

    @SerializedName("id")
    public String key;

    public String product;

    public String category;

    public String component;

    public String knob;

    public BugResolutionType resolution;

    @SerializedName("comment")
    public String resolutionComment;

    @SerializedName("cc")
    public String resolveCc;

    private String status;

    private boolean notFound;

    private List<BugComment> comments;

    private String webUrl;

    private String summary;

    private String description;

    public Bug(int key) {
        this.key = String.valueOf(key);
        this.notFound = true;
    }

    public Bug(Map values) throws IOException {
        this.key =  String.valueOf(values.get("bug_id"));
        this.product = (String) values.get("product");
        this.category = (String) values.get("category");
        this.component = (String) values.get("component");
        this.webUrl = String.valueOf(values.get("web_url"));
        this.summary = (String) values.get("short_desc");
        this.description = StringUtils.convertObjectToString(values.get("description"));
        this.comments = parseComments((Object[]) values.get("comments"));
        this.status = (String) values.get("bug_status");
        this.resolution = (BugResolutionType) findByValue(BugResolutionType.class, (String) values.get("resolution"));
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

    private List<BugComment> parseComments(Object[] commentObjects) throws IOException {
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
