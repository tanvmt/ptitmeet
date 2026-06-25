package com.ptithcm.ptitmeet.api.config;

import com.ptithcm.ptitmeet.BuildConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class ApiConfig {

    private ApiConfig() {
    }

    public static String getBaseUrl() {
        return BuildConfig.API_BASE_URL;
    }

    public static String getWebSocketUrl() {
        return getWebSocketUrl("chat", null);
    }

    public static String getWebSocketUrl(String service, String userId) {
        String baseUrl = getBaseUrl();
        String webSocketUrl = baseUrl.replaceFirst("^http://", "ws://")
                .replaceFirst("^https://", "wss://");

        if (webSocketUrl.endsWith("/")) {
            webSocketUrl = webSocketUrl.substring(0, webSocketUrl.length() - 1);
        }

        if (webSocketUrl.endsWith("/api")) {
            webSocketUrl = webSocketUrl.substring(0, webSocketUrl.length() - 4);
        }

        String endpoint = "meeting".equalsIgnoreCase(service) ? "/ws-meeting" : "/ws";
        String url = webSocketUrl + endpoint;
        if (userId != null && !userId.trim().isEmpty()) {
            url += "?userId=" + encodeQueryValue(userId.trim());
        }
        return url;
    }

    private static String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            return value;
        }
    }
}
