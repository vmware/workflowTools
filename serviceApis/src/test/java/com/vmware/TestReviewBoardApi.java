package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.cookie.CookieFileStore;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.reviewboard.domain.ReviewRequests;
import com.vmware.reviewboard.domain.RootList;
import com.vmware.reviewboard.domain.UserReview;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestReviewBoardApi extends BaseTests {

    private String repository;

    private ReviewBoard reviewBoard;
    private ReviewRequestDraft sampleRequest;
    private ReviewRequest createdRequest;
    private String reviewGroup;

    @Before
    public void setup() {
        repository = testProperties.getProperty("reviewboard.repository");
        reviewGroup = testProperties.getProperty("reviewboard.reviewGroup");
        String reviewboardUrl = testProperties.getProperty("reviewboard.url");
        String username = testProperties.getProperty("reviewboard.username");

        reviewBoard = new ReviewBoard(reviewboardUrl, username);
        reviewBoard.setupAuthenticatedConnection();
        reviewBoard.updateServerTimeZone("yyyy-MM-dd'T'HH:mm:ss");

        String NO_GROUPS = "";
        sampleRequest = new ReviewRequestDraft("Summary", "Description", "Testing", "HS-1054", username, NO_GROUPS, "testBranch");

    }

    @After
    public void cleanup() {
        if (createdRequest != null) {
            createdRequest.status = ReviewRequestStatus.discarded;
            reviewBoard.updateReviewRequest(createdRequest);
        }
    }

    @Test
    public void reviewboardIs17OrGreater() throws IOException, URISyntaxException {
        String version = reviewBoard.getVersion();
        assertTrue("Expected review board version of 1.7 or greater", version.compareTo("1.7") >=0);
    }

    @Test
    public void retrieveUserSessionToken() throws IOException {
        String userHome = System.getProperty( "user.home" );
        CookieFileStore cookieFileStore = new CookieFileStore(userHome);
        assertNotNull("SessionId should not be null", cookieFileStore.getCookie(ApiAuthentication.reviewBoard));
    }

    @Test
    public void getRootLinkList() throws IOException, URISyntaxException {
        RootList rootLinkList = reviewBoard.getRootLinkList();
        assertNotNull(rootLinkList.getReviewRequestsLink());
    }

    @Test
    public void getReviewRequests() throws IOException, URISyntaxException {
        ReviewRequests reviewRequests = reviewBoard.getReviewRequests(ReviewRequestStatus.all);
        assertTrue(reviewRequests.review_requests.length > 0);
    }

    @Test
    public void getReviewRequestsForGroup() throws IOException, URISyntaxException {
        long sevenDaysAgo = new Date().getTime() - TimeUnit.DAYS.toMillis(7);
        ReviewRequests reviewRequests =
                reviewBoard.getReviewRequestsWithShipItsForGroups(reviewGroup, new Date(sevenDaysAgo));
        assertTrue(reviewRequests.review_requests.length > 0);
    }

    @Test
    public void getReviewRequestById() {
        String jsonText = new ConfiguredGsonBuilder().build().toJson(sampleRequest);
        System.out.println(jsonText);
        createdRequest = reviewBoard.createReviewRequestFromDraft(sampleRequest, repository);

        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(createdRequest.id);
        assertNotNull(reviewRequest);
        assertTrue(reviewRequest.id != 0);
        assertEquals("Id mismatch", createdRequest.id, reviewRequest.id);
    }

    @Test
    public void getReviewShipItComment() throws IOException, URISyntaxException {
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(520886);
        reviewBoard.getSoftSubmitReview(reviewRequest);
    }

    @Test
    public void getUserReviewsForReviewRequest() throws IOException, URISyntaxException {
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(478638);
        UserReview[] reviews = reviewBoard.getReviewsForReviewRequest(reviewRequest.getReviewsLink());
        assertTrue(reviews.length == 1);

        assertTrue(reviews[0].isPublic);
        assertTrue(reviews[0].ship_it);
        assertEquals("malmond", reviews[0].getReviewUsername());
    }

    @Test
    public void createReviewRequest() {
        createdRequest = reviewBoard.createReviewRequestFromDraft(sampleRequest, repository);
    }

    @Test
    public void publishReview() {
        createdRequest = reviewBoard.createReviewRequestFromDraft(sampleRequest, repository);
        assertFalse("Review should not be published", createdRequest.isPublic);
        reviewBoard.publishReview(createdRequest.getDraftLink());
        ReviewRequest publishedReview = reviewBoard.getReviewRequestById(createdRequest.id);
        assertTrue("Review should be published", publishedReview.isPublic);
    }

    @Test
    public void getOpenReviewsWithShipIts() throws Exception {
        ReviewRequest[] reviewRequests = reviewBoard.getOpenReviewRequestsWithShipIts().review_requests;
        assertTrue(reviewRequests.length > 0);
    }

    @Test
    public void canDiscardReviewRequest() {
        createdRequest = reviewBoard.createReviewRequestFromDraft(sampleRequest, repository);
        createdRequest.status = ReviewRequestStatus.discarded;
        reviewBoard.updateReviewRequest(createdRequest);

        ReviewRequest updatedRequest = reviewBoard.getReviewRequestById(createdRequest.id);
        assertEquals("Status mismatch", ReviewRequestStatus.discarded, updatedRequest.status);
        createdRequest = null;
    }

    @Test(expected = NotFoundException.class)
    public void cannotGetNonExistentReviewRequest() throws IOException, URISyntaxException {
        reviewBoard.getReviewRequestById(-1);
    }

    @Test
    public void readDiffsForReviewRequest() {
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(601612);

        ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
        assertTrue("Diffs length mismatch", diffs.length == 1);

        String diffData = reviewBoard.getDiffData(diffs[diffs.length - 1].getSelfLink());
        assertTrue("Invalid diff", diffData.startsWith("diff --git"));
    }

}
