package com.vmware.action.trello;

import com.vmware.ServiceLocator;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.Swimlane;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionDescription("Syncs trello cards with list of loaded jira issues. " +
        "Adds / Deletes trello cards to ensure board matches loaded list.")
public class SyncCardsWithJiraIssues extends AbstractTrelloAction {

    public SyncCardsWithJiraIssues(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        trello = ServiceLocator.getTrello(config.trelloUrl);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (projectIssues.isEmpty()) {
            log.info("No jira issues loaded. No need to create trello cards.");
            return;
        }

        if (selectedBoard == null) {
            throw new IllegalArgumentException
                    ("No trello board has been loaded or created. Add a CreateTrelloBoard or SelectTrelloBoard actions.");

        }

        Padder padder = new Padder("Adding cards for board {}", selectedBoard.name);

        padder.infoTitle();
        List<Issue> issuesForProcessing = new ArrayList<Issue>(projectIssues.getIssuesForProcessing());
        log.info("Processing {} issues", issuesForProcessing.size());

        List<Card> existingCards = new ArrayList<Card>(Arrays.asList(trello.getCardsForBoard(selectedBoard)));
        if (!config.keepMissingCards) {
            deleteMissingCards(issuesForProcessing, existingCards);
        }
        filterOutExistingIssues(issuesForProcessing, existingCards);

        addCardsForIssues(issuesForProcessing);

        padder.infoTitle();
        log.info("Workflow setStoryPoints can be used to update jira issues " +
                "with story point values after estimating in trello is finished");
    }

    private void deleteMissingCards(List<Issue> issuesForProcessing, List<Card> existingCards) throws IllegalAccessException, IOException, URISyntaxException {
        int cardRemovalCount = 0;
        for (int i = existingCards.size() - 1; i >= 0; i--) {
            Card cardToCheck = existingCards.get(i);
            if (findIssueByKey(issuesForProcessing, cardToCheck.getIssueKey()) == null) {
                if (cardToCheck.getIssueKey() != null) {
                    log.debug("Card with name {} and key {} is not in issues list, deleting",
                            cardToCheck.name, cardToCheck.getIssueKey());
                } else {
                    log.debug("Card with name {} has no jira issue url, deleting", cardToCheck.name);
                }

                trello.deleteCard(cardToCheck);
                existingCards.remove(i);
                cardRemovalCount++;
            }
        }

        if (cardRemovalCount > 0) {
            log.info("Deleted {} cards that did not have a matching jira issue", cardRemovalCount);
        } else {
            log.debug("Deleted {} cards that did not have a matching jira issue", cardRemovalCount);
        }
    }

    private Issue findIssueByKey(List<Issue> issues, String key) {
        for (Issue issue : issues) {
            if (issue.getKey().equals(key)) {
                return issue;
            }
        }
        return null;
    }

    private void addCardsForIssues(List<Issue> issuesForProcessing) throws IOException, URISyntaxException, IllegalAccessException {
        if (issuesForProcessing.isEmpty()) {
            log.info("No cards need to be added to trello as issue list is now empty");
            return;
        }

        log.info("Adding {} cards to trello", issuesForProcessing.size());
        Swimlane[] swimlanes = trello.getSwimlanesForBoard(selectedBoard);
        Map<Integer, Swimlane> storyPointSwimlanes = convertSwimlanesIntoMap(swimlanes);

        Swimlane todoLane = swimlanes[0];
        for (Issue issueToAdd : issuesForProcessing) {
            Swimlane swimlaneToUse = todoLane;
            if (issueToAdd.fields.storyPoints != null) {
                Integer storyPointValue = issueToAdd.fields.storyPoints.intValue();
                swimlaneToUse = storyPointSwimlanes.get(storyPointValue);
            }

            Card cardToAdd = new Card(swimlaneToUse, issueToAdd, config.jiraUrl);

            log.debug("Adding card for issue {} to swimlane {}", issueToAdd.getKey(), swimlaneToUse.name);
            trello.createCard(cardToAdd);
        }
    }

    private Map<Integer, Swimlane> convertSwimlanesIntoMap(Swimlane[] swimlanes) {
        Map<Integer, Swimlane> storyPointSwimlanes = new HashMap<Integer, Swimlane>();
        for (Swimlane swimlane : swimlanes) {
            if (swimlane.getStoryPointValue() != null) {
                storyPointSwimlanes.put(swimlane.getStoryPointValue(), swimlane);
            }
        }
        return storyPointSwimlanes;
    }

    private void filterOutExistingIssues(List<Issue> issuesForProcessing, List<Card> existingCards) throws IOException, URISyntaxException {
        int issueRemovalCount = 0;
        for (int i = issuesForProcessing.size() - 1; i >= 0; i--) {
            Issue issueToCheck = issuesForProcessing.get(i);
            Card matchingCard = new Card(issueToCheck, config.jiraUrl);
            if (existingCards.contains(matchingCard)) {
                log.debug("Issue {} already exists in trello, skipping.", issueToCheck.id);
                issuesForProcessing.remove(i);
                issueRemovalCount++;
            }
        }

        if (issueRemovalCount > 0) {
            log.info("Filtered out {} issues that already existed in trello", issueRemovalCount);
        } else {
            log.debug("Filtered out {} issues that already existed in trello", issueRemovalCount);
        }
    }
}
