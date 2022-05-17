package com.vmware;

import com.vmware.jira.domain.Issue;
import com.vmware.trello.Trello;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.Member;
import com.vmware.trello.domain.Swimlane;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTrelloApi extends BaseTests {

    private Trello trello;

    private Board testBoard;

    @Before
    public void init() {
        String trelloUrl = testProperties.getProperty("trello.url");
        trello = new Trello(trelloUrl, "dummy", false, null, null);
        trello.setupAuthenticatedConnection();
        testBoard = trello.createBoard(new Board("Test Board"));
    }

    @After
    public void cleanup() {
        trello.closeBoard(testBoard);
    }

    @Test
    public void canGetTrelloMember() throws IOException, URISyntaxException {
        Member member = trello.getTrelloMember("me");
        assertNotNull(member);
    }

    @Test
    public void canGetUserBoards() {
        Board[] boards = trello.getOpenBoardsForUser();
        assertTrue("Expected user to have trello boards", boards.length > 0);
    }

    @Test
    public void canCreateDefaultSwimlanesIfNecessary() {
        trello.createDefaultSwimlanesIfNeeded(testBoard, Arrays.asList(1D,1.5D,3D,5D,8D,13D));
    }


    @Test
    public void canGetCards() {
        Board[] boards = trello.getOpenBoardsForUser();
        assertTrue("Expected user to have trello boards", boards.length > 0);
        Card[] cards = trello.getCardsForBoard(boards[0]);
        assertTrue("Expected board " + boards[0].name + " to have cards", cards.length > 0);
    }

    @Test
    public void boardIsPartOfUserBoards() {
        Board[] boards = trello.getOpenBoardsForUser();
        assertTrue("Expected user to have trello boards", boards.length > 0);

        boolean found = Arrays.stream(boards).anyMatch(board -> board.id.equals(testBoard.id));
        assertTrue("Expected test board to be part of user's boards", found);
    }

    @Test
    public void canCreateAndDeleteCard() {
        Swimlane swimlaneToCreate = new Swimlane(testBoard, "2 Story points");
        Swimlane createdSwimlane = trello.createSwimlane(swimlaneToCreate);

        Card cardToCreate = new Card(createdSwimlane, "Test Card");
        Card createdCard = trello.createCard(cardToCreate);
        assertNotNull(createdCard);
        assertEquals(cardToCreate.name, createdCard.name);

        Card[] cards = trello.getCardsForSwimlane(createdSwimlane);
        assertEquals("Swimlane cards count mismatch", 1, cards.length);

        assertEquals(cards[0].id, createdCard.id);

        trello.deleteCard(cards[0]);

        cards = trello.getCardsForSwimlane(createdSwimlane);
        assertEquals("Swimlane cards count mismatch", 0, cards.length);
    }

    @Test
    public void canParseCard() throws Exception {
        Issue sampleIssue = new Issue("HW-1001");
        sampleIssue.fields.acceptanceCriteria = "Acceptance";
        sampleIssue.fields.description = "Describe";
        sampleIssue.fields.summary = "Summation";
        sampleIssue.fields.storyPoints = 5;

        Swimlane swimlane = new Swimlane(new Board("test board"), "3 Story Point(s)");
        Card sampleCard = new Card(swimlane, sampleIssue, "https://jira.com");

        assertEquals(sampleIssue.fields.summary, sampleCard.name);
        assertEquals(sampleIssue.fields.description,
                sampleCard.getDescriptionWithoutJiraUrl("https://jira.com/browse/HW-1001"));
        assertEquals(sampleIssue.fields.acceptanceCriteria, sampleCard.getAcceptanceCriteria());
    }
}
