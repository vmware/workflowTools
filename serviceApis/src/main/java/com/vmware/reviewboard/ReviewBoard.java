package com.vmware.reviewboard;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.UrlParam;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.ApiToken;
import com.vmware.reviewboard.domain.ApiTokenRequest;
import com.vmware.reviewboard.domain.ApiTokenResponseEntity;
import com.vmware.reviewboard.domain.GeneralCommentsResponse;
import com.vmware.reviewboard.domain.ReviewComment;
import com.vmware.reviewboard.domain.DiffCommentsResponse;
import com.vmware.reviewboard.domain.DiffToUpload;
import com.vmware.reviewboard.domain.Link;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.RepositoryResponse;
import com.vmware.reviewboard.domain.ResultsCount;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.reviewboard.domain.ReviewRequestDiffsResponse;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.reviewboard.domain.ReviewRequestDraftResponse;
import com.vmware.reviewboard.domain.ReviewRequestResponse;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.reviewboard.domain.ReviewRequests;
import com.vmware.reviewboard.domain.ReviewUser;
import com.vmware.reviewboard.domain.ReviewUsersResponse;
import com.vmware.reviewboard.domain.RootList;
import com.vmware.reviewboard.domain.ServerInfo;
import com.vmware.reviewboard.domain.ServerInfoResponse;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.reviewboard.domain.UserReviewsResponse;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.vmware.http.cookie.ApiAuthentication.reviewBoard_cookie;
import static com.vmware.http.cookie.ApiAuthentication.reviewBoard_token;
import static com.vmware.http.request.RequestHeader.AUTHORIZATION;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;
import static com.vmware.reviewboard.domain.ReviewRequestDraft.aDraftForPublishingAReview;
import static com.vmware.reviewboard.domain.ReviewRequestStatus.all;
import static com.vmware.reviewboard.domain.ReviewRequestStatus.pending;

public class ReviewBoard extends AbstractRestService {
    private ServerInfo cachedServerInfo = null;
    private RootList cachedRootList = null;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public ReviewBoard(String reviewboardUrl, String username, ApiAuthentication credentialsType) {
        super(reviewboardUrl, "api/", credentialsType, username);
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);
        if (credentialsType == reviewBoard_token) {
            File tokenFile = new File(System.getProperty("user.home") + File.separator + credentialsType.getFileName());
            if (tokenFile.exists()) {
                log.debug("Using reviewboard access token file {}", tokenFile.getAbsolutePath());
                connection.removeCookie(reviewBoard_cookie.getCookieName());
                connection.addStatefulParam(RequestHeader.aTokenAuthHeader(IOUtils.read(tokenFile)));
            }
        }
    }

    public RootList getRootLinkList() {
        if (cachedRootList == null) {
            cachedRootList = connection.get(apiUrl, RootList.class);
        }
        return cachedRootList;
    }

    public ReviewRequests getReviewRequests(ReviewRequestStatus status) {
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class,
                new UrlParam("from-user", getUsername()), new UrlParam("status", status.name()));
    }

    public ReviewRequest[] getOpenReviewRequestsWithSubmittedComment() {
        List<ReviewRequest> reviewRequestsWithSubmittedComments = new ArrayList<ReviewRequest>();
        for (ReviewRequest reviewRequest : getOpenReviewRequestsWithShipIts().review_requests) {
            UserReview softSubmitReview = getSoftSubmitReview(reviewRequest);
            if (softSubmitReview != null) {
                reviewRequestsWithSubmittedComments.add(reviewRequest);
            }
        }
        return reviewRequestsWithSubmittedComments.toArray(new ReviewRequest[reviewRequestsWithSubmittedComments.size()]);
    }

    public ReviewRequests getOpenReviewRequestsWithShipIts() {
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class, new UrlParam("from-user", getUsername()),
                new UrlParam("status", pending.name()), new UrlParam("ship-it", "1"));
    }

    public List<ReviewUser> searchUsersMatchingText(Link usersLink, String textToMatch, boolean searchByUsernameOnly) {
        List<RequestParam> params = new ArrayList<>();
        params.add(new UrlParam("limit", "15"));
        params.add(new UrlParam("q", textToMatch));
        if (!searchByUsernameOnly) {
            params.add(new UrlParam("fullname", "1"));
        }
        return get(usersLink.getHref(), ReviewUsersResponse.class, params.toArray(new RequestParam[0])).users;
    }

    public int getFilesCountForReviewRequestDiff(Link filesLink) {
        return get(filesLink.getHref(), ResultsCount.class, new UrlParam("counts-only", "1")).count;
    }

    public ReviewRequests getReviewRequestsWithShipItsForGroups(String groupNames, Date fromDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(fromDate);
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class,
                new UrlParam("to-groups", groupNames), new UrlParam("max-results", "200"),
                new UrlParam("time-added-from", formattedDate),
                new UrlParam("ship-it", "1"), new UrlParam("status", all.name()));
    }

    public ReviewRequest getReviewRequestById(Integer id) {
        if (id == null) {
            return null;
        }
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        reviewRequestLink.addPathParam(String.valueOf(id));
        return get(reviewRequestLink.getHref(), ReviewRequestResponse.class).review_request;
    }

    public ReviewRequest createReviewRequest(String repository) {
        Link createLink = getReviewRequests(pending).getCreateLink();

        ReviewRequest initialReviewRequest = new ReviewRequest();
        initialReviewRequest.repository = repository;

        return connection.post(createLink.getHref(), ReviewRequestResponse.class, initialReviewRequest).review_request;
    }

    public ReviewRequest createReviewRequestFromDraft(ReviewRequestDraft reviewRequestDraft, String repository) {
        ReviewRequest addedRequest = createReviewRequest(repository);
        updateReviewRequestDraft(addedRequest.getDraftLink(), reviewRequestDraft);
        return getReviewRequestById(addedRequest.id);
    }

    public ReviewRequestDraft getReviewRequestDraft(Link draftLink) {
        return get(draftLink.getHref(), ReviewRequestDraftResponse.class).draft;
    }

    public ReviewRequestDraft getReviewRequestDraftWithExceptionHandling(Link draftLink) {
        try {
            return connection.get(draftLink.getHref(), ReviewRequestDraftResponse.class).draft;
        } catch (NotFoundException nfe) {
            log.debug("No draft found for link {}: {}", draftLink, nfe.getMessage());
            return null;
        }
    }

    public ReviewRequestDraft updateReviewRequestDraft(Link draftLink, ReviewRequestDraft draft) {
        String existingTestingDone = draft.testingDone;
        draft.testingDone = draft.fullTestingDoneSectionWithoutJobResults();

        ReviewRequestDraft updatedDraft = connection.put(draftLink.getHref(), ReviewRequestDraftResponse.class, draft).draft;
        draft.testingDone = existingTestingDone;
        return updatedDraft;
    }

    public void publishReview(Link draftLink, String changeDescription, boolean trivial) {
        connection.put(draftLink.getHref(), aDraftForPublishingAReview(changeDescription, trivial));
    }

    public void updateReviewRequest(ReviewRequest reviewRequest) {
        connection.put(reviewRequest.getUpdateLink().getHref(), ReviewRequestResponse.class, reviewRequest);
    }

    public void createReviewRequestDiff(Link diffLink, DiffToUpload diffToCreate) {
        post(diffLink.getHref(), diffToCreate);
    }

    public void createUserReview(ReviewRequest reviewRequest, UserReview review) {
        post(reviewRequest.getReviewsLink().getHref(), review);
    }

    public UserReview[] getReviewsForReviewRequest(Link reviewsLink) {
        return get(reviewsLink.getHref(), UserReviewsResponse.class).reviews;
    }

    public ReviewRequestDiff[] getDiffsForReviewRequest(Link diffsLink) {
        return get(diffsLink.getHref(), ReviewRequestDiffsResponse.class).diffs;
    }

    public ReviewComment[] getDiffCommentsForReview(Link diffCommentsLink) {
        ReviewComment[] comments = get(diffCommentsLink.getHref(), DiffCommentsResponse.class).diffComments;
        return comments != null ? comments : new ReviewComment[0];
    }

    public ReviewComment[] getGeneralCommentsForReview(Link commentsLink) {
        ReviewComment[] comments = get(commentsLink.getHref(), GeneralCommentsResponse.class).generalComments;
        return comments != null ? comments : new ReviewComment[0];
    }

    public Repository getRepository(Link repositoryLink) {
        return get(repositoryLink.getHref(), RepositoryResponse.class).repository;
    }

    public String getDiffData(Link diffLink) {
        String diffData = get(diffLink.getHref(), String.class, anAcceptHeader("text/x-patch"));
        // need to add in a trailing newline for git apply to work correctly
        diffData += "\n";
        return diffData;
    }

    public UserReview getSoftSubmitReview(ReviewRequest reviewRequest) {
        UserReview[] reviews = this.getReviewsForReviewRequest(reviewRequest.getReviewsLink());
        for (UserReview review : reviews) {
            if (review.getReviewUsername().equals(reviewRequest.getSubmitter())) {
                if (review.body_top.startsWith("Submitted as ")) {
                    return review;
                }
            }
        }
        return null;
    }

    public ServerInfo getServerInfo() {
        if (cachedServerInfo == null) {
            cachedServerInfo = connection.get(getRootLinkList().getInfoLink().getHref(), ServerInfoResponse.class).info;
        }
        return cachedServerInfo;
    }

    public boolean supportsDiffWithRenames() {
        return getVersion().compareTo("1.7") >= 0;
    }

    public boolean supportsDiffBaseCommitId() {
        return getVersion().compareTo("1.7.13") >= 0;
    }

    public void updateClientTimeZone(String serverDateFormat) {
        String serverTimeZone = getServerInfo().site.serverTimeZone;
        connection.updateTimezoneAndFormat(TimeZone.getTimeZone(serverTimeZone), serverDateFormat);
    }

    public void setupAuthenticatedConnectionWithLocalTimezone(String reviewBoardDateFormat) {
        if (isConnectionAuthenticated()) {
            return;
        }
        super.setupAuthenticatedConnection();
        updateClientTimeZone(reviewBoardDateFormat);
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType, getUsername());
        connection.setupBasicAuthHeader(credentials);

        if (credentialsType == reviewBoard_token) {
            String tokenUrl = UrlUtils.addRelativePaths(apiUrl, "users", getUsername(), "api-tokens/");
            String tokenValue;
            try {
                ApiToken apiToken = connection.post(tokenUrl, ApiTokenResponseEntity.class, new ApiTokenRequest()).apiToken;
                tokenValue = apiToken.token;
            } catch (NotAuthorizedException nae) {
                log.info("Failed to login with username and password, enter API token manually");
                tokenValue = InputUtils.readValue("Enter API token (Create in UI under My Account -> Authentication -> API Tokens");
            }
            saveApiToken(tokenValue, credentialsType);
            connection.addStatefulParam(new RequestHeader(AUTHORIZATION, "token " + tokenValue));
        }
    }

    @Override
    public void checkAuthenticationAgainstServer() {
        File tokenFile = new File(System.getProperty("user.home") + File.separator + credentialsType.getFileName());
        if (credentialsType == reviewBoard_token && !tokenFile.exists() && connection.hasCookie(reviewBoard_cookie)) {
            String tokenUrl = UrlUtils.addRelativePaths(apiUrl, "users", getUsername(), "api-tokens/");
            ApiToken apiToken = connection.post(tokenUrl, ApiTokenResponseEntity.class, new ApiTokenRequest()).apiToken;
            saveApiToken(apiToken.token, credentialsType);
            connection.addStatefulParam(RequestHeader.aTokenAuthHeader(apiToken.token));
            connection.removeCookie(reviewBoard_cookie.getCookieName());
        } else {
            getServerInfo();
        }
        if (credentialsType == reviewBoard_cookie && !connection.hasCookie(reviewBoard_cookie)) {
            log.warn("Cookie {} should have been retrieved from reviewboard login!", reviewBoard_cookie.getCookieName());
        }
    }

    private String getVersion() {
        return getServerInfo().product.version;
    }

}
