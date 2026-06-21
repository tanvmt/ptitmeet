package com.ptithcm.ptitmeet.api.realtime;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class StompSocketClient {

    public interface ConnectionListener {
        void onConnected();

        void onError(String message);

        void onDisconnected();
    }

    public interface MessageListener {
        void onMessage(String destination, String body);
    }

    private static final String NULL_CHAR = "\u0000";

    private final OkHttpClient okHttpClient;
    private final String webSocketUrl;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger subscriptionCounter = new AtomicInteger(1);
    private final Map<String, Subscription> pendingSubscriptions = new LinkedHashMap<>();
    private final Map<String, Subscription> activeSubscriptions = new LinkedHashMap<>();
    private final StringBuilder incomingBuffer = new StringBuilder();

    private WebSocket webSocket;
    private boolean connected;
    private ConnectionListener connectionListener;

    public StompSocketClient(OkHttpClient okHttpClient, String webSocketUrl) {
        this.okHttpClient = okHttpClient;
        this.webSocketUrl = webSocketUrl;
    }

    public void connect(ConnectionListener listener) {
        this.connectionListener = listener;
        Request request = new Request.Builder().url(webSocketUrl).build();
        webSocket = okHttpClient.newWebSocket(request, new SocketListener());
    }

    public void disconnect() {
        connected = false;
        pendingSubscriptions.clear();
        activeSubscriptions.clear();
        incomingBuffer.setLength(0);
        if (webSocket != null) {
            sendFrame("DISCONNECT\n\n" + NULL_CHAR);
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String subscribe(String destination, MessageListener listener) {
        String subscriptionId = "sub-" + subscriptionCounter.getAndIncrement();
        Subscription subscription = new Subscription(subscriptionId, destination, listener);
        if (connected) {
            activeSubscriptions.put(subscriptionId, subscription);
            sendSubscribeFrame(subscription);
        } else {
            pendingSubscriptions.put(subscriptionId, subscription);
        }
        return subscriptionId;
    }

    public void unsubscribe(String subscriptionId) {
        pendingSubscriptions.remove(subscriptionId);
        Subscription active = activeSubscriptions.remove(subscriptionId);
        if (active != null && connected) {
            sendFrame("UNSUBSCRIBE\nid:" + subscriptionId + "\n\n" + NULL_CHAR);
        }
    }

    public void send(String destination, String body) {
        if (!connected) {
            return;
        }
        String payload = body == null ? "" : body;
        int contentLength = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        sendFrame("SEND\ndestination:" + destination + "\ncontent-type:application/json\ncontent-length:" + contentLength + "\n\n" + payload + NULL_CHAR);
    }

    private void onSocketConnected() {
        connected = true;
        for (Subscription subscription : pendingSubscriptions.values()) {
            activeSubscriptions.put(subscription.id, subscription);
            sendSubscribeFrame(subscription);
        }
        pendingSubscriptions.clear();
        if (connectionListener != null) {
            mainHandler.post(connectionListener::onConnected);
        }
    }

    private void onSocketDisconnected() {
        connected = false;
        activeSubscriptions.clear();
        if (connectionListener != null) {
            mainHandler.post(connectionListener::onDisconnected);
        }
    }

    private void onSocketError(String message) {
        if (connectionListener != null) {
            mainHandler.post(() -> connectionListener.onError(message));
        }
    }

    private void sendConnectFrame() {
        sendFrame("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n" + NULL_CHAR);
    }

    private void sendSubscribeFrame(Subscription subscription) {
        sendFrame("SUBSCRIBE\nid:" + subscription.id + "\ndestination:" + subscription.destination + "\n\n" + NULL_CHAR);
    }

    private void sendFrame(String frame) {
        if (webSocket != null) {
            webSocket.send(frame);
        }
    }

    private void consumeIncoming(String text) {
        incomingBuffer.append(text);
        int terminatorIndex;
        while ((terminatorIndex = incomingBuffer.indexOf(NULL_CHAR)) >= 0) {
            String frame = incomingBuffer.substring(0, terminatorIndex);
            incomingBuffer.delete(0, terminatorIndex + 1);
            handleFrame(frame);
        }
    }

    private void handleFrame(String rawFrame) {
        if (rawFrame == null) {
            return;
        }

        String frame = rawFrame.trim();
        if (frame.isEmpty()) {
            return;
        }

        String[] sections = frame.split("(\\r?\\n){2}", 2);
        String headerBlock = sections[0];
        String body = sections.length > 1 ? sections[1].trim() : "";
        String[] headerLines = headerBlock.split("\\r?\\n");
        String command = headerLines[0].trim();

        Map<String, String> headers = new LinkedHashMap<>();
        for (int index = 1; index < headerLines.length; index++) {
            String line = headerLines[index].trim();
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }

        if ("CONNECTED".equals(command)) {
            onSocketConnected();
            return;
        }

        if ("MESSAGE".equals(command)) {
            String subscriptionId = headers.get("subscription");
            String destination = headers.get("destination");
            Subscription subscription = activeSubscriptions.get(subscriptionId);
            if (subscription != null) {
                mainHandler.post(() -> subscription.listener.onMessage(destination, body));
            }
            return;
        }

        if ("ERROR".equals(command)) {
            String message = headers.containsKey("message") ? headers.get("message") : body;
            onSocketError(message);
        }
    }

    private static class Subscription {
        private final String id;
        private final String destination;
        private final MessageListener listener;

        private Subscription(String id, String destination, MessageListener listener) {
            this.id = id;
            this.destination = destination;
            this.listener = listener;
        }
    }

    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            sendConnectFrame();
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            consumeIncoming(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            onSocketDisconnected();
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            onSocketDisconnected();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
            connected = false;
            onSocketError(t.getMessage() != null ? t.getMessage() : "WebSocket failure");
        }
    }
}
