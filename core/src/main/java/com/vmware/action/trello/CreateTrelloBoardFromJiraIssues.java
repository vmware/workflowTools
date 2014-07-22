package com.vmware.action.trello;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractBatchIssuesAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.trello.Trello;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.Swimlane;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionDescription("Creates a Trello board from a list of previously loaded jira issues")
public class CreateTrelloBoardFromJiraIssues extends AbstractBatchIssuesAction {

    Trello trello;

    public CreateTrelloBoardFromJiraIssues(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        trello = ServiceLocator.getTrello(config.trelloUrl);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (projectIssues.isEmpty()) {
            log.info("No jira issues loaded. Cannot create trello board.");
            return;
        }

        Board boardToCreate = new Board(projectIssues.projectName);

        Padder padder = new Padder("Trello Board {}", boardToCreate.name);
        padder.infoTitle();
        log.info("Creating trello board");

        Board createdBoard = trello.createBoard(boardToCreate);
        Swimlane[] swimlanes = trello.getSwimlanesForBoard(createdBoard);

        Swimlane todoLane = swimlanes[0];
        for (int i = 1; i < swimlanes.length; i ++) {
            log.info("Closing unneeded board {}", swimlanes[i].name);
            trello.closeSwimlane(swimlanes[i]);
        }

        Map<Integer, Swimlane> storyPointSwimlanes = new HashMap<Integer, Swimlane>();
        for (Integer storyPointValue : config.storyPointValues) {
            Swimlane swimlaneToCreate = new Swimlane(createdBoard, storyPointValue + Swimlane.STORY_POINTS_SUFFIX);
            log.info("Creating swimlane {}", swimlaneToCreate.name);
            storyPointSwimlanes.put(storyPointValue, trello.createSwimlane(swimlaneToCreate));
        }

        trello.createSwimlane(new Swimlane(createdBoard, "Parking Lot"));

        List<Issue> issuesForProcessing = projectIssues.getIssuesForProcessing();
        log.info("Adding {} cards", issuesForProcessing.size());
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

        log.info("Finished adding cards to board");
        log.info("Workflow setStoryPoints can be used to update jira issues " +
                "with story point values after estimating in trello is finished");
        padder.infoTitle();

    }
}
