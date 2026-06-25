export const SYSTEM_ACTION_TYPES = {
    MEETING_ENDED: "MEETING_ENDED",
    END_MEETING_FOR_ALL: "END_MEETING_FOR_ALL",
    HOST_TRANSFERRED: "HOST_TRANSFERRED",
    RECORDING_STARTED: "RECORDING_STARTED",
    RECORDING_STOPPED: "RECORDING_STOPPED",
    MUTE_ALL: "MUTE_ALL",
    STOP_CAMERA_ALL: "STOP_CAMERA_ALL",
    KICK_ALL: "KICK_ALL",
    MUTE_PARTICIPANT: "MUTE_PARTICIPANT",
    STOP_CAMERA_PARTICIPANT: "STOP_CAMERA_PARTICIPANT",
    KICK_PARTICIPANT: "KICK_PARTICIPANT",
};

const normalizeSystemActionType = (type) =>
    type === SYSTEM_ACTION_TYPES.END_MEETING_FOR_ALL
        ? SYSTEM_ACTION_TYPES.MEETING_ENDED
        : type;

export const getWebSocketUrl = (service = 'chat', userId = null) => {
    const apiUrl = import.meta.env.VITE_API_URL || "http://localhost:8080/api";
    const url = new URL(apiUrl);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.pathname = url.pathname.replace(/\/api\/?$/, "") + (service === 'meeting' ? "/ws-meeting" : "/ws");
    if (userId) {
        url.searchParams.set("userId", userId);
    }
    try {
        const userJson = localStorage.getItem("user");
        if (!userId && userJson) {
            const user = JSON.parse(userJson);
            if (user && (user.userId || user.id)) {
                url.searchParams.set("userId", user.userId || user.id);
            }
        }
    } catch (e) {}
    url.hash = "";
    return url.toString();
};

export const createSystemActionPayload = (type, extra = {}) => {
    const targetUserId = extra.targetUserId || extra.targetParticipantId;

    return JSON.stringify({
        type,
        action: type,
        ...extra,
        ...(targetUserId ? { targetUserId } : {}),
    });
};

export const parseSystemAction = (rawMessage) => {
    if (typeof rawMessage !== "string") {
        return null;
    }

    const trimmed = rawMessage.trim();
    if (!trimmed) {
        return null;
    }

    if (!trimmed.startsWith("{")) {
        return { type: normalizeSystemActionType(trimmed) };
    }

    try {
        const parsed = JSON.parse(trimmed);
        const type = normalizeSystemActionType(parsed.type || parsed.action);
        if (!type) {
            return null;
        }

        const targetParticipantId = parsed.targetParticipantId || parsed.targetUserId;
        return {
            ...parsed,
            type,
            action: parsed.action || type,
            ...(targetParticipantId ? { targetParticipantId } : {}),
            ...(targetParticipantId ? { targetUserId: parsed.targetUserId || targetParticipantId } : {}),
        };
    } catch (error) {
        console.error("Unable to parse system action payload:", error);
        return null;
    }
};
