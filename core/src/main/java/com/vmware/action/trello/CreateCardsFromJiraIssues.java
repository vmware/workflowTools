package com.vmware.action.trello;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractTrelloAction;
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

@ActionDescription("Creates trello cards for a list of previously loaded jira issues." +
        "Cards are only created for issues not already in Trello.")
public class CreateCardsFromJiraIssues extends AbstractTrelloAction {

    public CreateCardsFromJiraIssues(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
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

        if (!selectedBoard.hasId()) {
            throw new IllegalArgumentException
                    ("No trello board has been loaded or created. Add a CreateTrelloBoard or SelectTrelloBoard actions.");

        }

        Padder padder = new Padder("Adding cards for board {}", selectedBoard.name);

        List<Issue> issuesForProcessing = new ArrayList<Issue>(projectIssues.getIssuesForProcessing());
        log.info("Processing {} cards", issuesForProcessing.size());

        filterOutExistingIssues(issuesForProcessing);

        addCardsForIssues(issuesForProcessing);

        log.info("Finished adding cards to board");
        log.info("Workflow setStoryPoints can be used to update jira issues " +
                "with story point values after estimating in trello is finished");
        padder.infoTitle();

    }

    private void addCardsForIssues(List<Issue> issuesForProcessing) throws IOException, URISyntaxException, IllegalAccessException {
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

            log.debug("Adding card for issue {} to swimlane {}", issueToAdd.key, swimlaneToUse.name);
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

    private void filterOutExistingIssues(List<Issue> issuesForProcessing) throws IOException, URISyntaxException {
        int issueRemovalCount = 0;
        List<Card> existingCards = Arrays.asList(trello.getCardsForBoard(selectedBoard));
        for (int i = issuesForProcessing.size() - 1; i >= 0; i--) {
            Issue issueToCheck = issuesForProcessing.get(i);
            Card matchingCard = new Card(issueToCheck, config.jiraUrl);
            if (existingCards.contains(matchingCard)) {
                log.debug("Issue {} already exists in Trello, skipping.", issueToCheck.id);
                issuesForProcessing.remove(i);
                issueRemovalCount++;
            }
        }

        if (issueRemovalCount > 0) {
            log.info("Filtered out {} issues that already exists in Trello", issueRemovalCount);
        }
    }
}
