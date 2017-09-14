/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.trello;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.Cookie;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.BadRequestException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.UrlParam;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.trello.domain.AuthCode;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.BooleanValue;
import com.vmware.trello.domain.Card;
import com.vmware.trello.domain.LoginInfo;
import com.vmware.trello.domain.Member;
import com.vmware.trello.domain.SessionAuthentication;
import com.vmware.trello.domain.StringValue;
import com.vmware.trello.domain.Swimlane;
import com.vmware.trello.domain.TokenApproval;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.http.cookie.ApiAuthentication.trello;
import static com.vmware.http.credentials.UsernamePasswordAsker.askUserForUsernameAndPassword;
import static com.vmware.http.request.RequestHeader.aRefererHeader;

public class Trello extends AbstractRestService {

    private final String loginUrl;
    private final String sessionUrl;
    private final String webUrl;

    public Trello(String trelloUrl) {
        super(createApiUrl(trelloUrl), "1/", trello, null);
        webUrl = UrlUtils.addTrailingSlash(trelloUrl);
        this.loginUrl = webUrl + "1/authentication";
        this.sessionUrl = webUrl + "1/authorization/session";
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);

        String apiToken = readExistingApiToken(credentialsType);

        connection.addStatefulParamsFromUrlFragment(apiToken);
    }

    public Member getTrelloMember(String memberId) {
        return connection.get(apiUrl + "members/" + memberId, Member.class);
    }

    public Board createBoard(Board boardToCreate) {
        return connection.post(apiUrl + "boards", Board.class, boardToCreate);
    }

    public void closeBoard(Board board) {
        String url = String.format("%sboards/%s/closed", apiUrl, board.id);
        connection.put(url, new BooleanValue(true));
    }

    public Swimlane createSwimlane(Swimlane swimlaneToCreate) {
        return connection.post(apiUrl + "lists", Swimlane.class, swimlaneToCreate);
    }

    public void closeSwimlane(Swimlane swimlane) {
        String url = String.format("%slists/%s/closed", apiUrl, swimlane.id);
        connection.put(url, new BooleanValue(true));
    }

    public void moveSwimlane(Swimlane swimlane, String position) {
        String url = String.format("%slists/%s/pos", apiUrl, swimlane.id);
        connection.put(url, new StringValue(position));
    }

    public Card createCard(Card cardToCreate) {
        return connection.post(apiUrl + "cards", Card.class, cardToCreate);
    }

    public void deleteCard(Card cardToDelete) {
        String url = String.format(apiUrl + "cards/%s", cardToDelete.id);
        connection.delete(url);
    }

    public Board[] getOpenBoardsForUser() {
        return connection.get(apiUrl + "members/me/boards", Board[].class,
                    new UrlParam("filter", "open"));
    }

    public Swimlane[] getSwimlanesForBoard(Board board) {
        String url = String.format("%sboards/%s/lists", apiUrl, board.id);
        return connection.get(url, Swimlane[].class);
    }

    public Card[] getCardsForSwimlane(Swimlane swimlane) {
        String url = String.format("%slists/%s/cards", apiUrl, swimlane.id);
        return connection.get(url, Card[].class);
    }

    public Card[] getCardsForBoard(Board board) {
        String url = String.format("%sboards/%s/cards", apiUrl, board.id);
        return connection.get(url, Card[].class);
    }

    public void createDefaultSwimlanesIfNeeded(Board board, List<Double> storyPointValues) {
        List<Swimlane> existingSwimlanes = Arrays.asList(getSwimlanesForBoard(board));
        List<String> defaultSwimlaneNames = new ArrayList<>();
        defaultSwimlaneNames.add("To Do");
        storyPointValues.forEach(storyPoint -> defaultSwimlaneNames.add(createDisplayValue(storyPoint)));
        defaultSwimlaneNames.add("Parking Lot");

        List<String> existingSwimlaneNames = existingSwimlanes.stream()
                .map(swimlane -> swimlane.name).collect(Collectors.toList());
        if (defaultSwimlaneNames.equals(existingSwimlaneNames)) {
            log.info("Swimlanes on board match expected values");
            return;
        }

        for (Swimlane existingSwimlane : existingSwimlanes) {
            boolean matchesDefaultSwimlaneName = defaultSwimlaneNames.stream()
                    .anyMatch(name -> name.equalsIgnoreCase(existingSwimlane.name));
            if (!matchesDefaultSwimlaneName) {
                log.info("Deleting unneeded swimlane {}", existingSwimlane.name);
                closeSwimlane(existingSwimlane);
            }
        }

        for (int i = 0; i < defaultSwimlaneNames.size(); i++) {
            String swimLaneName = defaultSwimlaneNames.get(i);
            Optional<Swimlane> matchingSwimlane = existingSwimlanes.stream()
                    .filter(swimlane -> swimlane.name.equalsIgnoreCase(swimLaneName)).findFirst();
            String position = String.valueOf(i);
            if (!matchingSwimlane.isPresent()) {
                createSwimlane(board, swimLaneName, position);
            } else if (!position.equalsIgnoreCase(matchingSwimlane.get().pos)) {
                Swimlane swimlane = matchingSwimlane.get();
                log.info("Moving swimlane {} from position {} to {}", swimlane.name, swimlane.pos, position);
                moveSwimlane(swimlane, position);
            }
        }
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        try {
            getOpenBoardsForUser();
        } catch (BadRequestException e) {
            // authorization failure is handled as an invalid token bad request
            throw new NotAuthorizedException(e.getMessage());
        }
    }

    @Override
    protected void loginManually() {
        connection.resetParams();
        UsernamePasswordCredentials credentials = askUserForUsernameAndPassword(trello);
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        connection.setUseSessionCookies(true);
        // Trello seems to be particular over what is used as a dsc value, this one is copied from the browser
        String dscValue = "eac1c0ee5fbaf55de92323e7abf75e4b19888ce430f95afe83e63885f8959eff";
        connection.addCookie(new Cookie("dsc", dscValue));
        AuthCode authCode = connection.post(loginUrl, AuthCode.class, new LoginInfo(credentials), aRefererHeader("https://trello.com/login"));
        connection.post(sessionUrl, new SessionAuthentication(authCode.code, dscValue), aRefererHeader("https://trello.com/login?returnUrl=%2Flogged-out"));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);
        String apiTokenPage = connection.get(webUrl + "app-key", String.class);
        List<UrlParam> authQueryParams = scrapeAuthInfoFromUI(apiTokenPage);
        connection.setUseSessionCookies(false);

        connection.addStatefulParams(authQueryParams);

        saveApiToken(StringUtils.appendWithDelimiter("", authQueryParams, "&"), trello);
    }

    private void createSwimlane(Board board, String displayValue, String position) {
        Swimlane swimlaneToCreate = new Swimlane(board, displayValue);
        swimlaneToCreate.pos = position;
        log.info("Creating swimlane {} at position {}", swimlaneToCreate.name, position);
        createSwimlane(swimlaneToCreate);
    }

    private String createDisplayValue(Double storyPointValue) {
        String displayValue = String.valueOf(storyPointValue);
        int storyPointValueAsInt = storyPointValue.intValue();
        if (storyPointValueAsInt == storyPointValue) {
            displayValue = String.valueOf(storyPointValueAsInt);
        }
        return displayValue + Swimlane.STORY_POINTS_SUFFIX;
    }

    private List<UrlParam> scrapeAuthInfoFromUI(String apiTokenPage) {
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
        throw new FatalException("No match for pattern " + pattern);
    }

    private static String createApiUrl(String trelloUrl) {
        String fullUrl = UrlUtils.addTrailingSlash(trelloUrl);
        if (!fullUrl.contains("api.trello")) {
            fullUrl = fullUrl.replaceFirst("trello", "api.trello");
        }
        return fullUrl;
    }
}
