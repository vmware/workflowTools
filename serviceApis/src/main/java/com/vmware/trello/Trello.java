/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.trello;

import com.vmware.AbstractRestService;
import com.vmware.chrome.ChromeDevTools;
import com.vmware.chrome.SsoClient;
import com.vmware.chrome.domain.ApiRequest;
import com.vmware.chrome.domain.ApiResponse;
import com.vmware.config.section.SsoConfig;
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
import com.vmware.util.ThreadUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.http.cookie.ApiAuthentication.trello_token;
import static com.vmware.http.credentials.UsernamePasswordAsker.askUserForUsernameAndPassword;
import static com.vmware.http.request.RequestHeader.aRefererHeader;
import static java.lang.String.format;

public class Trello extends AbstractRestService {

    private static final String SSO_LOGIN_BUTTON = "use-sso-button";


    private final String loginUrl;
    private final String sessionUrl;
    private final String webUrl;
    private final boolean trelloSso;
    private final String ssoEmail;
    private final SsoConfig ssoConfig;

    public Trello(String trelloUrl, String username, boolean trelloSso, String ssoEmail, SsoConfig ssoConfig) {
        super(createApiUrl(trelloUrl), "1/", trello_token, username);
        webUrl = UrlUtils.addTrailingSlash(trelloUrl);
        this.trelloSso = trelloSso;
        this.ssoEmail = ssoEmail;
        this.ssoConfig = ssoConfig;
        this.loginUrl = webUrl + "1/authentication";
        this.sessionUrl = webUrl + "1/authorization/session";
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);

        String apiToken = readExistingApiToken(credentialsType);

        connection.addStatefulParamsFromUrlFragment(apiToken);
    }

    public Member getTrelloMember(String memberId) {
        return get(apiUrl + "members/" + memberId, Member.class);
    }

    public Board createBoard(Board boardToCreate) {
        return post(apiUrl + "boards", Board.class, boardToCreate);
    }

    public void closeBoard(Board board) {
        String url = String.format("%sboards/%s/closed", apiUrl, board.id);
        connection.put(url, new BooleanValue(true));
    }

    public Swimlane createSwimlane(Swimlane swimlaneToCreate) {
        return post(apiUrl + "lists", Swimlane.class, swimlaneToCreate);
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
        return post(apiUrl + "cards", Card.class, cardToCreate);
    }

    public void deleteCard(Card cardToDelete) {
        String url = String.format(apiUrl + "cards/%s", cardToDelete.id);
        connection.delete(url);
    }

    public Board[] getOpenBoardsForUser() {
        return get(apiUrl + "members/me/boards", Board[].class,
                    new UrlParam("filter", "open"));
    }

    public Swimlane[] getSwimlanesForBoard(Board board) {
        String url = String.format("%sboards/%s/lists", apiUrl, board.id);
        return get(url, Swimlane[].class);
    }

    public Card[] getCardsForSwimlane(Swimlane swimlane) {
        String url = String.format("%slists/%s/cards", apiUrl, swimlane.id);
        return get(url, Card[].class);
    }

    public Card[] getCardsForBoard(Board board) {
        String url = String.format("%sboards/%s/cards", apiUrl, board.id);
        return get(url, Card[].class);
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
        if (trelloSso) {
            Consumer<ChromeDevTools> ssoNavigateFunction = devTools -> {
                log.info("Using email {} for Trello SSO login", ssoEmail);
                ThreadUtils.sleep(1, TimeUnit.SECONDS);
                devTools.setValueById("user", ssoEmail.substring(0, ssoEmail.length() - 1));
                devTools.evaluateById("user", ".focus()");
                devTools.sendMessage(ApiRequest.sendInput(ssoEmail.substring(ssoEmail.length() - 1)));
                ThreadUtils.sleep(1, TimeUnit.SECONDS);
                devTools.clickById("login");
            };

            Function<ChromeDevTools, String> apiTokenGenerator = devTools -> {
                devTools.sendMessage(ApiRequest.navigate("https://trello.com/app-key/"));
                devTools.waitForDomContentEvent();
                String key = devTools.evaluateById("key", ".value").getValue();
                String href = devTools.evaluate("document.querySelectorAll('[data-track-direct-object=\"generate token link\"]')[0]", ".href", "a").getValue();
                devTools.sendMessage(ApiRequest.navigate(href));
                devTools.waitForDomContentEvent();
                devTools.clickById("approveButton");
                devTools.waitForDomContentEvent();
                ApiResponse apiToken = devTools.evaluate("document.getElementsByTagName(\"pre\")[0]", ".textContent", "pre");
                return String.format("key=%s&token=%s", key, apiToken.getValue());
            };

            String loginUrl = UrlUtils.addRelativePaths(baseUrl, "login");
            SsoClient ssoClient = new SsoClient(ssoConfig, getUsername(), ssoEmail);
            String keyAndToken = ssoClient.loginAndGetApiToken(webUrl + ".+?/boards", loginUrl, SSO_LOGIN_BUTTON, ssoNavigateFunction, apiTokenGenerator);
            connection.addStatefulParamsFromUrlFragment(keyAndToken);
            saveApiToken(keyAndToken, trello_token);
        } else {
            UsernamePasswordCredentials credentials = askUserForUsernameAndPassword(trello_token, getUsername());
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
            saveApiToken(StringUtils.appendWithDelimiter("", authQueryParams, "&"), trello_token);
        }

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
        String authorizeResponseText = connection.get(authorizeUrl, String.class, authorizeParams.toArray(new RequestParam[0]));

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
