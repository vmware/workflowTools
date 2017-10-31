package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

public class TrelloConfig {

    @ConfigurableProperty(commandLine = "-trelloUrl,--trello-url", help = "Url for trello server")
    public String trelloUrl;

    @ConfigurableProperty(help = "Swimlanes to use for trello. A list of story point values as integers is expected")
    public LinkedList<Double> storyPointValues;

    @ConfigurableProperty(commandLine = "-kmc,--keep-missing-cards", help = "Whether to not delete existing cards in Trello that do not match a loaded Jira issue")
    public boolean keepMissingCards;

    @ConfigurableProperty(commandLine = "-obo,--own-boards-only", help = "Disallow using a trello board owned by someone else")
    public boolean ownBoardsOnly;

    @ConfigurableProperty(help = "Adds review groups if a commit has file changes that match a file mapping")
    public Map<String, String> reviewGroupFileMappings = new TreeMap<>();
}
