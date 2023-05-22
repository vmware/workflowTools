package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

public class ReviewBoardConfig {

    @ConfigurableProperty(help = "Url for review board server", gitConfigProperty = "reviewboard.url")
    public String reviewboardUrl;

    @ConfigurableProperty(commandLine = "-reviewRepo,--review-board-repo", help = "Review board repository")
    public String reviewBoardRepository;

    @ConfigurableProperty(help = "Date format used by review board. Can change between review board versions")
    public String reviewBoardDateFormat;

    @ConfigurableProperty(commandLine = "--always-include-review-url", help = "Include review url for trivial commits as well")
    public boolean alwaysIncludeReviewUrl;

    @ConfigurableProperty(help = "Map of reviewer groups to select from for reviewed by section. E.g. create a techDebt group and list relevant reviewers")
    public LinkedHashMap<String, SortedSet<String>> reviewerGroups;

    @ConfigurableProperty(commandLine = "--disable-markdown", help = "Treat description and testing done as plain text")
    public boolean disableMarkdown;

    @ConfigurableProperty(commandLine = "-g,--groups", help = "Groups to set for the review request or for generating stats")
    public String[] targetGroups;

    @ConfigurableProperty(commandLine = "--old-submit", help = "Number of days after which to close old reviews that have been soft submitted")
    public int closeOldSubmittedReviewsAfter;

    @ConfigurableProperty(commandLine = "--old-ship-it", help = "Number of days after which to close old reviews that have ship its")
    public int closeOldShipItReviewsAfter;

    @ConfigurableProperty(commandLine = "-rid,--review-request-id", help = "Id of the review request to use")
    public String reviewRequestId;

    @ConfigurableProperty(commandLine = "--search-by-usernames-only", help = "Search reviewboard for users by username only")
    public boolean searchByUsernamesOnly;

    @ConfigurableProperty(commandLine = "--use-rb-api-token", help = "Use api loging for authenticaiton. Workflow tools will create an api token if one deesn't exist")
    public boolean useRbApiToken;

    @ConfigurableProperty(help = "Adds review groups if a commit has file changes that match a file mapping")
    public Map<String, String> reviewGroupFileMappings = new TreeMap<>();
}
