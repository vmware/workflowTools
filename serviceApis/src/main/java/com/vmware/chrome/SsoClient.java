package com.vmware.chrome;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vmware.chrome.domain.ApiRequest;
import com.vmware.chrome.domain.ApiResponse;
import com.vmware.config.section.SsoConfig;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;

import com.vmware.util.exception.FatalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsoClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final SsoConfig ssoConfig;
    private final String username;
    private final String email;

    public SsoClient(SsoConfig ssoConfig, String username, String email) {
        this.ssoConfig = ssoConfig;
        this.username = username;
        this.email = email;
    }

    public String loginAndGetApiToken(String siteUrl, String siteLoginUrl, String ssoLoginButtonId, Consumer<ChromeDevTools> ssoNavigateConsumer,
                                      Function<ChromeDevTools, String> authenticationTokenFunction) {
        Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck = new AbstractMap.SimpleEntry<>(ApiRequest.evaluate("window.location.href"),
                response -> response.matchesUrl(siteUrl));
        return loginAndGetApiToken(loggedInCheck, siteLoginUrl, ssoLoginButtonId, ssoNavigateConsumer, authenticationTokenFunction);
    }

    public String loginAndGetApiToken(Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck, String siteLoginUrl, String ssoLoginButtonId, Consumer<ChromeDevTools> ssoNavigateConsumer,
                                      Function<ChromeDevTools, String> authenticationTokenFunction) {
        ChromeDevTools devTools = ChromeDevTools.devTools(ssoConfig.chromePath, ssoConfig.ssoHeadless, ssoConfig.chromeDebugPort);
        devTools.sendMessage(new ApiRequest("Page.enable"));
        devTools.sendMessage(ApiRequest.navigate(siteLoginUrl));
        devTools.waitForDomContentEvent();
        ThreadUtils.sleep(2, TimeUnit.SECONDS);
        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, loggedInCheck, ssoLoginButtonId, ssoConfig.ssoSignInButtonId);

        if (response.matchesElementId(ssoLoginButtonId)) {
            log.info("Clicking SSO sign in element {}", ssoLoginButtonId);
            devTools.clickById(ssoLoginButtonId);
            ssoNavigateConsumer.accept(devTools);
            response = waitForSiteUrlOrSignInElements(devTools, loggedInCheck, ssoConfig.ssoSignInButtonId, "userNameFormSubmit");

            if (response.matchesElementId("userNameFormSubmit")) {
                if (StringUtils.isEmpty(email)) {
                    throw new FatalException("No email specified for sso, specify --sso-email property or set a value for git user.email");
                }
                log.info("Using email {} for SSO", email);
                devTools.setValueById(ssoConfig.emailAddressInputId, email);
                devTools.clickById(ssoConfig.emailAddressSubmitButtonId);
                devTools.waitForDomContentEvent();
                response = waitForSiteUrlOrSignInElements(devTools, loggedInCheck, ssoConfig.ssoSignInButtonId);
            }
        }

        if (loggedInCheck.getValue().test(response)) {
            log.info("Retrieving api token after successful login using SSO");
            String apiAuthentication = authenticationTokenFunction.apply(devTools);
            devTools.close();
            return apiAuthentication;
        }

        if (!ssoConfig.manualLoginConfigPresent()) {
            throw new RuntimeException("Not all properties for manual SSO login are configured, skipping");
        }

        log.info("Logging in via SSO login page");
        String apiToken = loginWithUsernameAndPassword(loggedInCheck, authenticationTokenFunction, devTools, 0);
        devTools.close();
        return apiToken;
    }

    private String loginWithUsernameAndPassword(Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck,
                                                Function<ChromeDevTools, String> apiAuthenticationFunction, ChromeDevTools devTools, int retry) {
        devTools.waitForAnyElementId(ssoConfig.ssoUsernameInputId);

        String passwordId = devTools.waitForAnyElementId(ssoConfig.ssoPasswordInputId, ssoConfig.ssoPasscodeInputId);
        if (passwordId.equals(ssoConfig.ssoPasscodeInputId)) {
            UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(ApiAuthentication.vcd, username,
                    "RSA Passcode");
            devTools.setValueById(ssoConfig.ssoUsernameInputId, credentials.getUsername());
            devTools.setValueById(ssoConfig.ssoPasscodeInputId, credentials.getPassword());
        } else {
            UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(ApiAuthentication.vcd, username);
            devTools.setValueById(ssoConfig.ssoUsernameInputId, credentials.getUsername());
            devTools.setValueById(ssoConfig.ssoPasswordInputId, credentials.getPassword());
        }

        String signInButtonId = devTools.waitForAnyElementId(ssoConfig.ssoSignInButtonId, ssoConfig.ssoPasscodeSignInButtonId);
        devTools.clickById(signInButtonId);
        List<String> elementsToClickAfterPasscodeSubmit = new ArrayList<>(Arrays.asList(ssoConfig.elementsToClickAfterSignIn));
        List<String> elementsToCheck = new ArrayList<>(elementsToClickAfterPasscodeSubmit);
        elementsToCheck.addAll(Arrays.asList(ssoConfig.ssoSignInButtonId, ssoConfig.ssoPasscodeSignInButtonId));
        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, loggedInCheck, elementsToCheck);

        Optional<String> elementToClickAfterSubmit = elementsToClickAfterPasscodeSubmit.stream().filter(response::matchesElementId).findFirst();
        while (elementToClickAfterSubmit.isPresent()) {
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
            log.info("Clicking {} button", elementToClickAfterSubmit.get());
            devTools.clickById(elementToClickAfterSubmit.get());
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
            elementsToCheck.remove(elementToClickAfterSubmit.get());
            response = waitForSiteUrlOrSignInElements(devTools, loggedInCheck, elementsToCheck);
            elementToClickAfterSubmit = elementsToClickAfterPasscodeSubmit.stream().filter(response::matchesElementId).findFirst();
        }

        if (loggedInCheck.getValue().test(response)) {
            log.info("Retrieving api token after successful login");
            return apiAuthenticationFunction.apply(devTools);
        } else {
            if (retry > 3) {
                throw new RuntimeException("Failed to login via SSO after " + retry + " retries");
            } else {
                log.info("Failed to login, retry {} of 3 times", retry + 1);
                return loginWithUsernameAndPassword(loggedInCheck, apiAuthenticationFunction, devTools, retry + 1);
            }
        }

    }

    private ApiResponse waitForSiteUrlOrSignInElements(ChromeDevTools devTools, Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck, String... elementIds) {
        return waitForSiteUrlOrSignInElements(devTools, loggedInCheck, Arrays.asList(elementIds));
    }

    private ApiResponse waitForSiteUrlOrSignInElements(ChromeDevTools devTools, Map.Entry<ApiRequest, Predicate<ApiResponse>> loggedInCheck, List<String> elementIds) {
        Map<ApiRequest, Predicate<ApiResponse>> signInButtonOrLoggedInCheckMap = new HashMap<>();
        elementIds.stream().filter(Objects::nonNull).forEach(elementId -> {
            signInButtonOrLoggedInCheckMap.put(ApiRequest.elementById(elementId),
                    response -> response.matchesElementId(elementId));
        });
        signInButtonOrLoggedInCheckMap.put(loggedInCheck.getKey(), loggedInCheck.getValue());

        return devTools.waitForAnyPredicate(signInButtonOrLoggedInCheckMap, 0, elementIds + " or " + loggedInCheck);
    }
}
