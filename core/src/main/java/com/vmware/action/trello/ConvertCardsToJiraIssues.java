package com.vmware.action.trello;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueType;
import com.vmware.jira.domain.IssueTypeDefinition;
import com.vmware.http.UrlUtils;
import com.vmware.trello.Trello;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.Swimlane;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Converts the selected board's cards into a list of jira issues.")
public class ConvertCardsToJiraIssues extends AbstractTrelloAction {
    private Trello trello;

    public ConvertCardsToJiraIssues(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        trello = serviceLocator.getTrello();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (selectedBoard == null) {
            log.info("No trello board is selected");
            return;
        }

        projectIssues.reset();

        Swimlane[] swimlanes = trello.getSwimlanesForBoard(selectedBoard);

        Padder padder = new Padder("{} Swimlanes", selectedBoard.name);
        padder.infoTitle();
        for (Swimlane swimlane : swimlanes) {
            Integer storyPointValue = swimlane.getStoryPointValue();
            if (storyPointValue == null) {
                log.info("Skipping issues in swimlane {}", swimlane.name);
                continue;
            }

            Card[] cardsToUpdate = trello.getCardsForSwimlane(swimlane);
            if (cardsToUpdate.length == 0) {
                log.info("No cards in swimlane {}", swimlane.name);
                continue;
            }

            log.info("{} cards to process for swimlane {}", cardsToUpdate.length, swimlane.name);

            for (Card cardToUpdate : cardsToUpdate) {
                Issue issueToUpdate = convertCardToIssue(storyPointValue, cardToUpdate);
                projectIssues.add(issueToUpdate);
            }
        }
        padder.infoTitle();
    }

    private Issue convertCardToIssue(Integer storyPointValue, Card cardToUpdate) {
        Issue issueToUpdate = new Issue(cardToUpdate.getIssueKey());
        issueToUpdate.fields.storyPoints = storyPointValue;
        issueToUpdate.fields.issuetype = new IssueType(IssueTypeDefinition.Story);
        issueToUpdate.fields.summary = cardToUpdate.name;
        String urlForIssue =
                UrlUtils.addTrailingSlash(config.jiraUrl) + "browse/" + issueToUpdate.getKey();
        issueToUpdate.fields.description = cardToUpdate.getDescriptionWithoutJiraUrl(urlForIssue);
        issueToUpdate.fields.acceptanceCriteria = cardToUpdate.getAcceptanceCriteria();
        return issueToUpdate;
    }

}
