package com.vmware.chrome;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
import com.vmware.util.IOUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.RuntimeTimeoutException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChromeDevTools extends WebSocketClient {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private AtomicInteger screenShotCounter = new AtomicInteger(1);

    private Gson gson = new Gson();
    private CompletableFuture<ApiResponse> eventFuture;
    private CompletableFuture<ApiResponse> domContentEventFuture;
    private int currentRequestId;

    public ChromeDevTools(URI endpointURI) {
        super(endpointURI);
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
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {
        throw new RuntimeException(e);
    }

    public void onMessage(ByteBuffer bytes) {
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public ApiResponse sendMessage(String message) {
        return sendMessage(new ApiRequest(message));
    }

    public ApiResponse evaluate(String element) {
        return sendMessage(ApiRequest.evaluate(element));
    }

    public ApiResponse evaluateById(String elementId, String operation) {
        String elementScript = String.format("document.getElementById('%s')", elementId);
        return evaluate(elementScript, operation, elementId);
    }

    public ApiResponse clickById(String elementId) {
        evaluate(String.format("document.getElementById('%s')", elementId), ".disabled = false","#" + elementId);
        return evaluate(String.format("document.getElementById('%s')", elementId), ".click()","#" + elementId);
    }

    public ApiResponse setValueById(String elementId, String value) {
        return evaluate(String.format("document.getElementById('%s')", elementId), ".value = '" + value + "'","#" + elementId);
    }

    public ApiResponse evaluate(String locator, String operation, String testDescription) {
        waitForPredicate(ApiRequest.evaluate(locator), new Predicate<ApiResponse>() {
            @Override
            public boolean test(ApiResponse response) {
                return response.getDescription() != null && response.getDescription().contains(testDescription);
            }
        }, 0, "element " + testDescription);
        return sendMessage(ApiRequest.evaluate(locator + operation));
    }

    public byte[] captureScreenshot() {
        String screenshotData = sendMessage("Page.captureScreenshot").getData();
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
}