package com.vmware.bugzilla.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.IssueInfo;
import com.vmware.rest.request.DeserializedName;
import com.vmware.rest.request.PostDeserialization;
import com.vmware.utils.MatcherUtils;
import com.vmware.utils.StringUtils;

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

    @DeserializedName("bug_id")
    @SerializedName("id")
    public String key;

    public String priority;

    @SerializedName("bug_severity")
    public String bugSeverity;

    public String product;

    public String category;

    @DeserializedName("product")
    @SerializedName("found_in_product_name")
    public String foundInProductName;

    @DeserializedName("found_in")
    @SerializedName("found_in_version_name")
    public String foundInVersionName = "";

    @SerializedName("short_desc")
    public String summary;

    public String component;

    public String knob;

    public BugResolutionType resolution;

    @SerializedName("comment")
    public String resolutionComment = "";

    @SerializedName("cc")
    public String resolveCc;

    @SerializedName("longdesclength")
    public int descriptionLength;

    public int changed;

    public String delta_ts = "";

    @DeserializedName("bug_status")
    @Expose(serialize = false)
    public String status;

    public BugComment[] comments;

    private boolean notFound = true;

    @DeserializedName("web_url")
    private String webUrl;

    private String description;

    public Bug() {}

    public Bug(int key) {
        this.key = String.valueOf(key);
    }

    @PostDeserialization
    public void calculateDerivedValues() {
        this.descriptionLength = description != null ? description.length() : 0;
        this.notFound = webUrl == null;
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

}
