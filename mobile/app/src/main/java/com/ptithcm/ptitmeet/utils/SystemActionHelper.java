package com.ptithcm.ptitmeet.utils;

import org.json.JSONObject;

public final class SystemActionHelper {

    private SystemActionHelper() {
    }

    public static String createPayload(String type) {
        return createPayload(type, null, null);
    }

    public static String createPayload(String type, String targetParticipantId, String targetParticipantName) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            if (targetParticipantId != null) {
                jsonObject.put("targetParticipantId", targetParticipantId);
            }
            if (targetParticipantName != null) {
                jsonObject.put("targetParticipantName", targetParticipantName);
            }
            return jsonObject.toString();
        } catch (Exception exception) {
            return "{\"type\":\"" + type + "\"}";
        }
    }
}
