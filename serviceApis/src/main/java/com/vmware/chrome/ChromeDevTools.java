package com.vmware.chrome;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
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
    private AtomicInteger screenCastCounter = new AtomicInteger(1);

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
            File screencastFile = new File("screencast" + screenCastCounter.getAndIncrement() + ".png");
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

    public ApiResponse evaluate(String elementId, String operation) {
        String elementScript = String.format("document.getElementById('%s')", elementId);
        waitForPredicate(ApiRequest.evaluate(elementScript), new Predicate<ApiResponse>() {
            @Override
            public boolean test(ApiResponse response) {
                return response.getDescrption() != null && response.getDescrption().contains(elementId);
            }
        }, 0, "element " + elementId);
        return sendMessage(ApiRequest.evaluate(elementScript + operation));
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

    public String waitForUrl(String url) {
        return waitForAnyOfUrls(Collections.singletonList(url));
    }

    public String waitForAnyOfUrls(List<String> urlsToWaitFor) {
        ApiResponse url = waitForPredicate(ApiRequest.evaluate("window.location.href"),
                response -> urlsToWaitFor.contains(response.getValue()), 0, "any of urls " + urlsToWaitFor);
        return url.getValue();
    }

    public ApiResponse waitForPredicate(ApiRequest apiRequest, Predicate<ApiResponse> predicate, int retry, String description) {
        return waitForAnyPredicate(Collections.singletonMap(apiRequest, predicate), retry, description);
    }

    public ApiResponse waitForAnyPredicate(Map<ApiRequest, Predicate<ApiResponse>> requestPredicateMap, int retry, String description) {
        Optional<ApiResponse> apiResponse = requestPredicateMap.entrySet().stream().map(entry -> {
            ApiResponse response = sendMessage(entry.getKey());
            return entry.getValue().test(response) ? response : null;
        }).filter(Objects::nonNull).findFirst();

        if (!apiResponse.isPresent()) {
            if (retry > 10) {
                log.info("Current url {}", evaluate("window.location.href").getValue());
                File screenshot = new File("failedSso.png");
                log.info("Saving screenshot to {}", screenshot.getAbsolutePath());
                byte[] screenshotData = captureScreenshot();
                IOUtils.write(screenshot, screenshotData);
                throw new RuntimeTimeoutException("Failed to wait for " + description + " in " + retry + " retries");
            }
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
            return waitForAnyPredicate(requestPredicateMap, retry + 1, description);
        } else {
            return apiResponse.get();
        }
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