package com.vmware.chrome;

import java.net.URI;
import java.nio.file.Files;
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
import com.vmware.chrome.domain.ChromeTab;
import com.vmware.config.section.SsoConfig;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.FileUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vmware.util.FileUtils.createTempDirectory;
import static java.lang.String.format;

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
        String headParam = ssoConfig.ssoHeadless ? " --headless" : " --no-first-run --disable-session-crashed-bubble --user-data-dir="
                + createTempDirectory("chromeSso", true).getAbsolutePath();
        String chromeCommand = "\"" + ssoConfig.chromePath + "\"" + headParam + " --remote-debugging-port=" + ssoConfig.chromeDebugPort;
        log.info("Using Google Chrome for SSO to get API token, launching with {}", chromeCommand);
        Process chromeProcess = CommandLineUtils.executeCommand(null, null, chromeCommand, (String) null);
        String startupText = IOUtils.readWithoutClosing(chromeProcess.getInputStream());
        log.debug(startupText);

        HttpConnection connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        ChromeTab chromeTab = connection.get("http://localhost:" + ssoConfig.chromeDebugPort + "/json/new?about:blank", ChromeTab.class);

        ChromeDevTools devTools = new ChromeDevTools(URI.create(chromeTab.getWebSocketDebuggerUrl()));
        devTools.sendMessage("Page.enable");
        devTools.sendMessage(ApiRequest.navigate(siteLoginUrl));
        devTools.waitForDomContentEvent();
        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoLoginButtonId, ssoConfig.ssoSignInButtonId);

        if (response.matchesElementId(ssoLoginButtonId)) {
            log.info("Clicking SSO sign in element {}", ssoLoginButtonId);
            devTools.clickById(ssoLoginButtonId);
            ssoNavigateConsumer.accept(devTools);
            response = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoConfig.ssoSignInButtonId, "userNameFormSubmit");

            if (response.matchesElementId("userNameFormSubmit")) {
                log.info("Using email {} for SSO", email);
                devTools.setValueById("userInput", email);
                devTools.clickById("userNameFormSubmit");
                devTools.waitForDomContentEvent();
                response = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoConfig.ssoSignInButtonId);
            }
        }

        if (response.matchesUrl(siteUrl)) {
            log.info("Retrieving api token after successful login using SSO");
            String apiAuthentication = authenticationTokenFunction.apply(devTools);
            devTools.close();
            chromeProcess.destroy();
            return apiAuthentication;
        }

        if (!ssoConfig.manualLoginConfigPresent()) {
            throw new RuntimeException("Not all properties for manual SSO login are configured, skipping");
        }

        log.info("Logging in via SSO login page");
        String apiToken = loginWithUsernameAndPassword(siteUrl, authenticationTokenFunction, devTools, 0);
        devTools.close();
        chromeProcess.destroy();
        return apiToken;
    }

    private String loginWithUsernameAndPassword(String siteUrl, Function<ChromeDevTools, String> apiAuthenticationFunction, ChromeDevTools devTools, int retry) {
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
        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, siteUrl, elementsToCheck);

        Optional<String> elementToClickAfterSubmit = elementsToClickAfterPasscodeSubmit.stream().filter(response::matchesElementId).findFirst();
        while (elementToClickAfterSubmit.isPresent()) {
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
            log.info("Clicking {} button", elementToClickAfterSubmit.get());
            devTools.clickById(elementToClickAfterSubmit.get());
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
            elementsToCheck.remove(elementToClickAfterSubmit.get());
            response = waitForSiteUrlOrSignInElements(devTools, siteUrl, elementsToCheck);
            elementToClickAfterSubmit = elementsToClickAfterPasscodeSubmit.stream().filter(response::matchesElementId).findFirst();
        }

        if (response.matchesUrl(siteUrl)) {
            log.info("Retrieving api token after successful login");
            return apiAuthenticationFunction.apply(devTools);
        } else {
            if (retry > 3) {
                throw new RuntimeException("Failed to login via SSO after " + retry + " retries");
            } else {
                log.info("Failed to login, retry {} of 3 times", retry + 1);
                return loginWithUsernameAndPassword(siteUrl, apiAuthenticationFunction, devTools, retry + 1);
            }
        }

    }

    private ApiResponse waitForSiteUrlOrSignInElements(ChromeDevTools devTools, String siteUrl, String... elementIds) {
        return waitForSiteUrlOrSignInElements(devTools, siteUrl, Arrays.asList(elementIds));
    }

    private ApiResponse waitForSiteUrlOrSignInElements(ChromeDevTools devTools, String siteUrl, List<String> elementIds) {
        Map<ApiRequest, Predicate<ApiResponse>> signInButtonOrSiteUrlMap = new HashMap<>();
        elementIds.stream().filter(Objects::nonNull).forEach(elementId -> {
            signInButtonOrSiteUrlMap.put(ApiRequest.elementById(elementId),
                    response -> response.matchesElementId(elementId));
        });
        signInButtonOrSiteUrlMap.put(ApiRequest.evaluate("window.location.href"),
                response -> response.matchesUrl(siteUrl));

        return devTools.waitForAnyPredicate(signInButtonOrSiteUrlMap, 0, elementIds.toString() + " or " + siteUrl);
    }
}
