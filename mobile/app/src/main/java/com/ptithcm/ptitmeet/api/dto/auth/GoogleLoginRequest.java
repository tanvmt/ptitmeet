package com.ptithcm.ptitmeet.api.dto.auth;

public class GoogleLoginRequest {

    private final String idToken;

    public GoogleLoginRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }
}
