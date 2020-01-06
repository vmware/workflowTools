package com.vmware.trello.domain;

import com.google.gson.annotations.Expose;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueFields;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public class Card {
    public String idList;
    public String name;
    public String desc;

    @Expose(serialize = false)
    public String id;
    @Expose(serialize = false)
    public String idBoard;
    public CardLabel[] labels;
    @Expose(serialize = false)
    public String shortUrl;
    @Expose(serialize = false)
    public String url;

    private Card() {}

    public Card(Swimlane swimlane, String name) {
        this.idList = swimlane.id;
        this.name = name;
    }

    public Card(Issue issue, String jiraUrl) {
        this(null, issue, jiraUrl);
    }

    public Card(Swimlane swimlane, Issue issue, String jiraUrl) {
        this.idList = swimlane != null ? swimlane.id : null;

        IssueFields details = issue.fields;
        this.name = details.summary;

        this.desc = "";
        if (StringUtils.isNotEmpty(details.acceptanceCriteria)) {
            this.desc += "**  Acceptance Criteria  **\n" + details.acceptanceCriteria + "\n\n";
        }

        this.desc += "**  Description  **\n" + details.description + "\n";

        String urlForIssue = UrlUtils.addTrailingSlash(jiraUrl) + "browse/" + issue.getKey();

        this.desc += urlForIssue;
    }

    public String getIssueKey() {
        return MatcherUtils.singleMatch(desc, "browse/([\\w-]+)");
    }

    public String getDescriptionWithoutJiraUrl(String jiraUrl) {
        String value = MatcherUtils.singleMatch(desc, "\\*\\*\\s*Description\\s*\\*\\*(.+)", Pattern.DOTALL);
        if (value == null) {
            // return all the description if there is no match
            return !desc.isEmpty() ? desc : "No description";
        }
        return value.endsWith(jiraUrl) ? value.substring(0, value.indexOf(jiraUrl)).trim() : value.trim();
    }

    public String getAcceptanceCriteria() {
        String value = MatcherUtils.singleMatch(desc,
                "\\*\\*\\s*Acceptance Criteria\\s*\\*\\*(.+)\\*\\*\\s*Description\\s*\\*\\*", Pattern.DOTALL);
        return value != null ? value.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;
        if (id != null && id.equals(card.id)) {
            return true;
        }

        if (name == null || desc == null) {
            return false;
        }

        if (name.equals(card.name) && desc.equals(card.desc)) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = idList != null ? idList.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (idBoard != null ? idBoard.hashCode() : 0);
        result = 31 * result + (labels != null ? Arrays.hashCode(labels) : 0);
        result = 31 * result + (shortUrl != null ? shortUrl.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
