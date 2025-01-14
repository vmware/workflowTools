package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

import java.util.LinkedList;

public class TrelloConfig {

    @ConfigurableProperty(help = "Url for trello server")
    public String trelloUrl;

    @ConfigurableProperty(help = "Swimlanes to use for trello. A list of story point values as integers is expected")
    public LinkedList<Double> storyPointValues;

    @ConfigurableProperty(commandLine = "-kmc,--keep-missing-cards", help = "Whether to not delete existing cards in Trello that do not match a loaded Jira issue")
    public boolean keepMissingCards;

    @ConfigurableProperty(commandLine = "-obo,--own-boards-only", help = "Disallow using a trello board owned by someone else")
    public boolean ownBoardsOnly;

    @ConfigurableProperty(commandLine = "--trello-sso", help = "Use SSO to sign in for Trello")
    public boolean trelloSso;

    @ConfigurableProperty(commandLine = "--trello-username", help = "Username for Trello if not using the default username")
    public String trelloUsername;
}
