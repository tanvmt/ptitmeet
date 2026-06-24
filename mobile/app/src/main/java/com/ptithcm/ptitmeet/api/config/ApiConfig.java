package com.ptithcm.ptitmeet.api.config;

import com.ptithcm.ptitmeet.BuildConfig;

public final class ApiConfig {

    private ApiConfig() {
    }

    public static String getBaseUrl() {
        return BuildConfig.API_BASE_URL;
    }

    public static String getWebSocketUrl() {
        String baseUrl = getBaseUrl();
        String webSocketUrl = baseUrl.replaceFirst("^http://", "ws://")
                .replaceFirst("^https://", "wss://");

        if (webSocketUrl.endsWith("/")) {
            webSocketUrl = webSocketUrl.substring(0, webSocketUrl.length() - 1);
        }

        if (webSocketUrl.endsWith("/api")) {
            webSocketUrl = webSocketUrl.substring(0, webSocketUrl.length() - 4);
        }

        return webSocketUrl + "/ws";
    }
}
