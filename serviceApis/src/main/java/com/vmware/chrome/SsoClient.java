package com.vmware.chrome;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import com.vmware.util.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class SsoClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private final SsoConfig ssoConfig;

    public SsoClient(SsoConfig ssoConfig) {
        this.ssoConfig = ssoConfig;
    }

    public String loginAndGetApiToken(String siteUrl, String ssoLoginButtonId) {
        String headlessText = ssoConfig.ssoHeadless ? " --headless" : "";
        String chromeCommand = "\"" + ssoConfig.chromePath + "\"" + headlessText + " --disable-gpu --remote-debugging-port=9223";
        log.info("Using Google Chrome for SSO to get API token, launching with {}", chromeCommand);
        Process chromeProcess = CommandLineUtils.executeCommand(null, null, chromeCommand, (String) null);
        String startupText = IOUtils.readWithoutClosing(chromeProcess.getInputStream());
        log.debug(startupText);

        HttpConnection connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        ChromeTab chromeTab = connection.get("http://localhost:9223/json/new?about:blank", ChromeTab.class);

        ChromeDevTools devTools = new ChromeDevTools(URI.create(chromeTab.getWebSocketDebuggerUrl()));
        devTools.sendMessage("Page.enable");
        devTools.sendMessage(ApiRequest.navigate(siteUrl));
        devTools.waitForDomContentEvent();
        //devTools.sendMessage(new ApiRequest("Page.startScreencast", Collections.singletonMap("everyNthFrame", 2)));
        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoLoginButtonId, ssoConfig.ssoSignInButtonId);
        if (siteUrl.equalsIgnoreCase(response.getValue())) {
            log.info("Retrieved api token using SSO");
            String apiToken = devTools.evaluate(ssoConfig.ssoApiTokenJavaScript).getValue();
            devTools.close();
            chromeProcess.destroy();
            return apiToken;
        }

        if (ssoLoginButtonId != null && response.getDescrption() != null && response.getDescrption().contains(ssoLoginButtonId)) {
            log.info("Clicking SSO sign in element {}", ssoLoginButtonId);
            devTools.evaluate(ssoLoginButtonId, ".click()");
            devTools.waitForDomContentEvent();
            ApiResponse loginResponse = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoConfig.ssoSignInButtonId);
            if (siteUrl.equals(loginResponse.getValue())) {
                log.info("Retrieved api token using SSO");
                String apiToken = devTools.evaluate(ssoConfig.ssoApiTokenJavaScript).getValue();
                devTools.close();
                chromeProcess.destroy();
                return apiToken;
            }
        }

        if (!ssoConfig.manualLoginConfigPresent()) {
            throw new RuntimeException("Not all properties for manual SSO login are configured, skipping");
        }

        log.info("Logging in via SSO login page");
        String apiToken = loginWithUsernameAndPassword(siteUrl, devTools, 0);
        devTools.close();
        chromeProcess.destroy();
        return apiToken;
    }

    private String loginWithUsernameAndPassword(String siteUrl, ChromeDevTools devTools, int retry) {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(ApiAuthentication.vcd);

        devTools.evaluate(ssoConfig.ssoUsernameInputId, format(".value = '%s'", credentials.getUsername()));
        devTools.evaluate(ssoConfig.ssoPasswordInputId, format(".value = '%s'", credentials.getPassword()));
        devTools.evaluate(ssoConfig.ssoSignInButtonId, ".disabled = false");
        devTools.evaluate(ssoConfig.ssoSignInButtonId, ".click()");

        ApiResponse response = waitForSiteUrlOrSignInElements(devTools, siteUrl, ssoConfig.ssoSignInButtonId);
        if (siteUrl.equals(response.getValue())) {
            return devTools.evaluate(ssoConfig.ssoApiTokenJavaScript).getValue();
        } else {
            if (retry > 3) {
                throw new RuntimeException("Failed to login via SSO after " + retry + " retries");
            } else {
                log.info("Failed to login, retry {} of 3 times", retry + 1);
                return loginWithUsernameAndPassword(siteUrl, devTools, retry + 1);
            }
        }

    }

    private ApiResponse waitForSiteUrlOrSignInElements(ChromeDevTools devTools, String siteUrl, String... elementIds) {
        Map<ApiRequest, Predicate<ApiResponse>> signInButtonOrSiteUrlMap = new HashMap<>();
        Arrays.stream(elementIds).filter(Objects::nonNull).forEach(elementId -> {
            signInButtonOrSiteUrlMap.put(ApiRequest.evaluate(String.format("document.getElementById('%s')", elementId)),
                    response -> response.getDescrption() != null && response.getDescrption().contains(elementId));
        });
        signInButtonOrSiteUrlMap.put(ApiRequest.evaluate("window.location.href"), response -> siteUrl.equalsIgnoreCase(response.getValue()));

        return devTools.waitForAnyPredicate(signInButtonOrSiteUrlMap, 0, Arrays.toString(elementIds) + " or " + siteUrl);
    }
}
