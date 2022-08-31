package com.vmware.chrome;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.vmware.chrome.domain.ApiRequest;
import com.vmware.chrome.domain.ApiResponse;
import com.vmware.chrome.domain.ChromeTab;
import com.vmware.http.HttpConnection;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeTimeoutException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vmware.util.FileUtils.createTempDirectory;

public class ChromeDevTools extends WebSocketClient {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private AtomicInteger screenShotCounter = new AtomicInteger(1);

    private Gson gson = new Gson();
    private CompletableFuture<ApiResponse> eventFuture;
    private CompletableFuture<ApiResponse> domContentEventFuture;
    private int currentRequestId;
    private Process chromeProcess;

    public static ChromeDevTools devTools(String chromePath, boolean ssoHeadless, int chromeDebugPort) {
        HttpConnection connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        Process chromeProcess = null;
        ChromeTab chromeTab = null;
        try {
            chromeTab = connection.get("http://127.0.0.1:" + chromeDebugPort + "/json/new?about:blank", ChromeTab.class);
        } catch (FatalException uhe) {
            chromeProcess = launchChrome(chromePath, ssoHeadless, chromeDebugPort);
            chromeTab = connection.get("http://127.0.0.1:" + chromeDebugPort + "/json/new?about:blank", ChromeTab.class);
        }

        return new ChromeDevTools(URI.create(chromeTab.getWebSocketDebuggerUrl()), chromeProcess);
    }

    private ChromeDevTools(URI endpointURI, Process chromeProcess) {
        super(endpointURI);
        this.chromeProcess = chromeProcess;
        try {
            super.connectBlocking();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    public void onMessage(String message) {
        ApiResponse response = gson.fromJson(message, ApiResponse.class);
        if ("Page.screencastFrame".equals(response.method) && response.getParamsData() != null) {
            File screencastFile = new File("screencast" + screenShotCounter.getAndIncrement() + ".png");
            log.debug("Saving screencast frame to {}", screencastFile.getAbsolutePath());
            IOUtils.write(screencastFile, Base64.getDecoder().decode(response.getParamsData()));
        } else if (response.getData() != null) {
            log.debug("Response: id {} data response", response.id);
            if (eventFuture != null && currentRequestId == response.id) {
                eventFuture.complete(response);
            }
        } else if (response.method != null) {
            if (domContentEventFuture != null && "Page.domContentEventFired".equals(response.method)) {
                domContentEventFuture.complete(response);
            }
        } else {
            log.debug("Response: {}", message);
            if (eventFuture != null && currentRequestId == response.id) {
                eventFuture.complete(response);
            } else if (eventFuture != null) {
                log.info("Failed to find matching response for id {}\n{}", currentRequestId, message);
            }
        }
    }

    @Override
    public void close() {
        super.close();
        if (chromeProcess != null) {
            chromeProcess.destroy();
        }
    }

    public void closeDevToolsOnly() {
        super.close();
    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {
        throw new RuntimeException(e);
    }

    public void onMessage(ByteBuffer bytes) {
    }

    public ApiResponse evaluate(String element) {
        return sendMessage(ApiRequest.evaluate(element));
    }

    public ApiResponse evaluateById(String elementId, String operation) {
        String elementScript = String.format("document.getElementById('%s')", elementId);
        return evaluate(elementScript, operation, elementId);
    }

    public ApiResponse clickById(String elementId) {
        return clickById(String.format("document.getElementById('%s')", elementId), "#" + elementId);
    }

    public ApiResponse clickById(String locator, String testDescription) {
        evaluate(locator, ".disabled = false",testDescription);
        return evaluate(locator, ".click()",testDescription);
    }

    public void setValueById(String elementId, String value) {
        evaluate(String.format("document.getElementById('%s')", elementId), ".value = '" + value + "'","#" + elementId);
        evaluate(String.format("document.getElementById('%s')", elementId), ".dispatchEvent(new Event('input', {bubbles: true}))","#" + elementId);
    }

    public String getValue(String elementScript) {
        return waitForPredicate(ApiRequest.evaluate(elementScript), apiResponse -> StringUtils.isNotBlank(apiResponse.getValue()), 0, elementScript).getValue();
    }

    public ApiResponse evaluate(String locator, String operation, String testDescription) {
        waitForPredicate(ApiRequest.evaluate(locator),
                response -> response.getDescription() != null && response.getDescription().contains(testDescription), 0, "element " + testDescription);
        return sendMessage(ApiRequest.evaluate(locator + operation));
    }

    public byte[] captureScreenshot() {
        String screenshotData = sendMessage(new ApiRequest("Page.captureScreenshot")).getData();
        return Base64.getDecoder().decode(screenshotData);
    }

    public ApiResponse sendMessage(ApiRequest message) {
        String request = gson.toJson(message);
        log.debug("Request: {}", request);
        super.send(request);
        this.currentRequestId = message.getId();
        this.eventFuture = new CompletableFuture<>();
        try {
            ApiResponse response = this.eventFuture.get();
            if (response.exceptionDetails != null) {
                throw new RuntimeException(response.exceptionDetails.toString());
            }
            return response;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public ApiResponse waitForPredicate(ApiRequest apiRequest, Predicate<ApiResponse> predicate, int retry, String description) {
        return waitForAnyPredicate(Collections.singletonMap(apiRequest, predicate), retry, description);
    }

    public String waitForAnyElementId(String... elementIds) {
        Map<ApiRequest, Predicate<ApiResponse>> requestPredicateMap = Arrays.stream(elementIds)
                .filter(Objects::nonNull).collect(Collectors.toMap(ApiRequest::elementById,
                        value -> response -> response.getDescription() != null && response.getDescription().contains("#" + value)));
        ApiResponse response = waitForAnyPredicate(requestPredicateMap, 0, Arrays.toString(elementIds));
        return Arrays.stream(elementIds).filter(elementId -> response.getDescription() != null && response.getDescription().contains("#" + elementId)).findFirst()
                .orElseThrow(() -> new RuntimeException("No element found matching " + Arrays.toString(elementIds)));
    }

    public ApiResponse waitForAnyPredicate(Map<ApiRequest, Predicate<ApiResponse>> requestPredicateMap, int retry, String description) {
        Optional<ApiResponse> apiResponse = requestPredicateMap.entrySet().stream().map(entry -> {
            ApiResponse response = sendMessage(entry.getKey());
            return entry.getValue().test(response) ? response : null;
        }).filter(Objects::nonNull).findFirst();

        if (!apiResponse.isPresent()) {
            if (retry > 10) {
                log.info("Current url {}", evaluate("window.location.href").getValue());
                saveScreenshot();
                throw new RuntimeTimeoutException("Failed to wait for " + description + " in " + retry + " retries");
            } else if (log.isTraceEnabled()) {
                saveScreenshot();
            }
            ThreadUtils.sleep(2, TimeUnit.SECONDS);
            return waitForAnyPredicate(requestPredicateMap, retry + 1, description);
        } else {
            return apiResponse.get();
        }
    }

    public void saveScreenshot() {
        File screenShot = new File("currentPage" + screenShotCounter.getAndIncrement() + ".png");
        log.info("Saving screenshot to {}", screenShot.getAbsolutePath());
        IOUtils.write(screenShot, captureScreenshot());
    }

    public ApiResponse waitForDomContentEvent() {
        domContentEventFuture = new CompletableFuture<>();
        try {
            return domContentEventFuture.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static Process launchChrome(String chromePath, boolean ssoHeadless, int chromeDebugPort) {
        String headParam = ssoHeadless ? "--headless" : "--no-first-run --disable-session-crashed-bubble --ignore-certificate-errors --user-data-dir="
                + createTempDirectory("chromeTemp", true).getAbsolutePath();
        String chromeCommand = "\"" + chromePath + "\" " + headParam + " --remote-debugging-port=" + chromeDebugPort;
        LoggerFactory.getLogger(ChromeDevTools.class).info("Launching Chrome with command {}", chromeCommand);
        Process chromeProcess = CommandLineUtils.executeCommand(null, null, chromeCommand, (String) null);
        String startupText = IOUtils.readWithoutClosing(chromeProcess.getInputStream());
        LoggerFactory.getLogger(ChromeDevTools.class).debug(startupText);
        return chromeProcess;
    }
}