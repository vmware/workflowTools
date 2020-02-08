package com.vmware.reviewboard.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewRequest extends BaseEntity {

    @Expose(serialize = false)
    public Integer id;
    public ReviewRequestStatus status;
    @Expose(serialize = false)
    public String branch;
    @SerializedName("bugs_closed")
    @Expose(serialize = false)
    private List<String> bugNumbers = new ArrayList<String>();
    @SerializedName("target_people")
    @Expose(serialize = false)
    private List<Link> targetUsers = new ArrayList<Link>();
    @SerializedName("target_groups")
    @Expose(serialize = false)
    public List<Link> targetGroups = new ArrayList<>();
    @Expose(serialize = false)
    public String summary;
    public String description;
    @SerializedName("testing_done")
    @Expose(serialize = false)
    public String testingDone;
    @SerializedName("time_added")
    @Expose(serialize = false)
    public Date timeAdded;
    @SerializedName("last_updated")
    @Expose(serialize = false)
    public Date lastUpdated;
    public String repository;
    @Expose(serialize = false)
    public Date timestamp;
    @Expose(serialize = false)
    @SerializedName(value = "public")
    public boolean isPublic;
    @SerializedName("commit_id")
    public String commitId;

    @Expose(serialize = false, deserialize = false)
    public Map<ReviewStatType, Long> stats = new HashMap<ReviewStatType, Long>();

    @Expose(serialize = false, deserialize = false)
    public UserReview[] userReviews;

    @Expose(serialize = false, deserialize = false)
    public ReviewRequestDiff[] diffs;

    @Expose(serialize = false, deserialize = false)
    public int fileCount;


    public ReviewRequest() {}

    public Link getDraftLink() {
        return getLink("draft");
    }

    public Link getDiffsLink() {
        return getLink("diffs");
    }

    public Link getReviewsLink() {
        return getLink("reviews");
    }

    public Link getRepositoryLink() {
        return getLink("repository");
    }

    public String getSubmitter() {
        return getLink("submitter").getTitle();
    }

    public String getBugNumbers() {
        if (bugNumbers.isEmpty()) {
            return "";
        }
        return StringUtils.join(bugNumbers);
    }

    public String getTargetReviewersAsString() {
        String reviewersAsString = "";
        for (Link user : targetUsers) {
            if (!reviewersAsString.isEmpty()) {
                reviewersAsString += ",";
            }
            reviewersAsString += user.getTitle();
        }
        return reviewersAsString;
    }

    public ReviewRequestDraft asDraft() {
        ReviewRequestDraft draft = new ReviewRequestDraft();
        draft.id = String.valueOf(id);
        draft.summary = summary;
        draft.description = description;
        draft.testingDone = testingDone;
        draft.reviewedBy = getTargetReviewersAsString();
        draft.bugNumbers = getBugNumbers();
        return draft;
    }

}
