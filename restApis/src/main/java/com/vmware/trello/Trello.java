/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.trello;

import com.vmware.AbstractRestService;
import com.vmware.rest.RestConnection;
import com.vmware.rest.UrlUtils;
import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.BadRequestException;
import com.vmware.rest.exception.NotAuthorizedException;
import com.vmware.rest.request.RequestBodyHandling;
import com.vmware.rest.request.RequestHeader;
import com.vmware.rest.request.RequestParam;
import com.vmware.rest.request.UrlParam;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.BooleanValue;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.LoginInfo;
import com.vmware.trello.domain.Swimlane;
import com.vmware.trello.domain.TokenApproval;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.rest.credentials.UsernamePasswordAsker.askUserForUsernameAndPassword;

public class Trello extends AbstractRestService {

    private final String loginUrl;
    private final String webUrl;
    private List<UrlParam> authQueryParams;

    public Trello(String trelloUrl) throws IOException, URISyntaxException, IllegalAccessException {
        super(createApiUrl(trelloUrl), "1/", ApiAuthentication.trello, null);
        webUrl = UrlUtils.addTrailingSlash(trelloUrl);
        this.loginUrl = webUrl + "authenticate";
        this.connection = new RestConnection(RequestBodyHandling.AsStringJsonEntity);

        String apiToken = readExistingApiToken();

        connection.addStatefulParams(UrlUtils.parseParamsFromText(apiToken));
    }

    public Board createBoard(Board boardToCreate) throws IllegalAccessException, IOException, URISyntaxException {
        return connection.post(apiUrl + "boards", Board.class, boardToCreate);
    }

    public void closeBoard(Board board) throws IllegalAccessException, IOException, URISyntaxException {
        String url = String.format("%sboards/%s/closed", apiUrl, board.id);
        connection.put(url, new BooleanValue(true));
    }

    public Swimlane createSwimlane(Swimlane swimlaneToCreate) throws IllegalAccessException, IOException, URISyntaxException {
        return connection.post(apiUrl + "lists", Swimlane.class, swimlaneToCreate);
    }

    public void closeSwimlane(Swimlane swimlane) throws IllegalAccessException, IOException, URISyntaxException {
        String url = String.format("%slists/%s/closed", apiUrl, swimlane.id);
        connection.put(url, new BooleanValue(true));
    }

    public Card createCard(Card cardToCreate) throws IllegalAccessException, IOException, URISyntaxException {
        return connection.post(apiUrl + "cards", Card.class, cardToCreate);
    }

    public void deleteCard(Card cardToDelete) throws IllegalAccessException, IOException, URISyntaxException {
        String url = String.format(apiUrl + "cards/%s", cardToDelete.id);
        connection.delete(url);
    }

    public Board[] getOpenBoardsForUser() throws IOException, URISyntaxException {
        return connection.get(apiUrl + "members/me/boards", Board[].class,
                    new UrlParam("filter", "open"));
    }

    public Swimlane[] getSwimlanesForBoard(Board board) throws IOException, URISyntaxException {
        String url = String.format("%sboards/%s/lists", apiUrl, board.id);
        return connection.get(url, Swimlane[].class);
    }

    public Card[] getCardsForSwimlane(Swimlane swimlane) throws IOException, URISyntaxException {
        String url = String.format("%slists/%s/cards", apiUrl, swimlane.id);
        return connection.get(url, Card[].class);
    }

    public Card[] getCardsForBoard(Board board) throws IOException, URISyntaxException {
        String url = String.format("%sboards/%s/cards", apiUrl, board.id);
        return connection.get(url, Card[].class);
    }

    @Override
    protected void checkAuthenticationAgainstServer() throws IOException, URISyntaxException {
        try {
            getOpenBoardsForUser();
        } catch (BadRequestException e) {
            // authorization failure is handled as an invalid token bad request
            throw new NotAuthorizedException(e.getMessage());
        }
    }

    @Override
    protected void loginManually() throws IllegalAccessException, IOException, URISyntaxException {
        connection.clearStatefulParams();
        UsernamePasswordCredentials credentials = askUserForUsernameAndPassword(ApiAuthentication.trello);
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        connection.post(loginUrl, new LoginInfo(credentials), new RequestHeader("Referer", "https://trello.com/login"));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);

        connection.setUseSessionCookies(true);
        String apiTokenPage = connection.get(webUrl + "app-key", String.class);
        authQueryParams = scrapeAuthInfoFromUI(apiTokenPage);
        connection.setUseSessionCookies(false);

        connection.addStatefulParams(authQueryParams);

        saveApiToken(StringUtils.appendWithDelimiter("", authQueryParams, "&"));
    }

    private List<UrlParam> scrapeAuthInfoFromUI(String apiTokenPage) throws IOException, URISyntaxException, IllegalAccessException {
        String apiKey = findMatchForPattern(apiTokenPage, "id=\"key\" type=\"text\" value=\"(\\w+)\"");

        String authorizeUrl = webUrl + "1/authorize";
        List<RequestParam> authorizeParams = new ArrayList<RequestParam>();
        authorizeParams.add(new UrlParam("response_type", "token"));
        authorizeParams.add(new UrlParam("key", apiKey));
        authorizeParams.add(new UrlParam("return_url", "https://trello.com"));
        authorizeParams.add(new UrlParam("callback_method", "postMessage"));
        authorizeParams.add(new UrlParam("scope", "read,write"));
        authorizeParams.add(new UrlParam("expiration", "30days"));
        authorizeParams.add(new UrlParam("name", "Workflow Tools"));
        String authorizeResponseText = connection.get(authorizeUrl, String.class, authorizeParams);

        String requestKey = findMatchForPattern(authorizeResponseText, "name=\"requestKey\" value=\"(\\w+)\"");
        String signature = findMatchForPattern(authorizeResponseText, "name=\"signature\" value=\"([\\w/]+)\"");

        TokenApproval tokenApproval = new TokenApproval(requestKey, signature);
        String tokenApproveUrl = webUrl + "1/token/approve";
        String tokenApprovedText = connection.post(tokenApproveUrl, String.class, tokenApproval);

        String accessToken = findMatchForPattern(tokenApprovedText, "postMessage\\(\"(\\w+)\"");
        return Arrays.asList(new UrlParam("key", apiKey), new UrlParam("token", accessToken));
    }

    private String findMatchForPattern(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.debug("Api page text\n{}", text);
        throw new IllegalArgumentException("No match for pattern " + pattern);
    }

    private static String createApiUrl(String trelloUrl) {
        String fullUrl = UrlUtils.addTrailingSlash(trelloUrl);
        if (!fullUrl.contains("api.trello")) {
            fullUrl = fullUrl.replaceFirst("trello", "api.trello");
        }
        return fullUrl;
    }
}
