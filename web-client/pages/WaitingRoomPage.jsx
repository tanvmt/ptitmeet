import React, { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { Client } from "@stomp/stompjs";
import { meetingService } from "../services/meetingService";
import {
  getDevicePermissionStates,
  getPermissionErrorMessage,
  requestDeviceAccess,
  stopMediaStream,
} from "../utils/mediaPermissions";
import { getWebSocketUrl } from "../utils/meetingRealtime";

const WaitingRoomPage = () => {
  const { code } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const isHostSetup = Boolean(location.state?.hostSetup);
  const [micOn, setMicOn] = useState(true);
  const [videoOn, setVideoOn] = useState(true);
  const [isCheckingDevices, setIsCheckingDevices] = useState(true);
  const [deviceError, setDeviceError] = useState("");
  const [permissionState, setPermissionState] = useState({
    microphone: "prompt",
    camera: "prompt",
  });
  const [previewStream, setPreviewStream] = useState(null);
  const [joinState, setJoinState] = useState("IDLE");
  const [errorMsg, setErrorMsg] = useState(null);
  const [waitingMessage, setWaitingMessage] = useState("Joining meeting...");
  const [stompClient, setStompClient] = useState(null);

  const videoRef = useRef(null);
  const mediaPreferenceRef = useRef({ micOn: true, videoOn: true });
  const previewStreamRef = useRef(null);

  useEffect(() => {
    mediaPreferenceRef.current = { micOn, videoOn };
  }, [micOn, videoOn]);

  useEffect(() => {
    previewStreamRef.current = previewStream;
  }, [previewStream]);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = previewStream;
    }
  }, [previewStream]);

  useEffect(() => {
    const initializePermissions = async () => {
      setIsCheckingDevices(true);

      try {
        const states = await getDevicePermissionStates();
        setPermissionState(states);

        if (states.camera === "granted") {
          const { stream } = await requestDeviceAccess({ video: true });
          setPreviewStream((previousStream) => {
            stopMediaStream(previousStream);
            return stream;
          });
        }
      } catch (error) {
        console.error("Unable to initialize device setup:", error);
      } finally {
        setIsCheckingDevices(false);
      }
    };

    initializePermissions();

    return () => {
      stopMediaStream(previewStreamRef.current);
    };
  }, []);

  useEffect(() => {
    return () => {
      if (stompClient && stompClient.active) stompClient.deactivate();
    };
  }, [stompClient]);

  const syncPermissions = async () => {
    const latestStates = await getDevicePermissionStates();
    setPermissionState(latestStates);
    return latestStates;
  };

  const requestPermissions = async ({ audio = false, video = false }) => {
    try {
      setDeviceError("");
      setIsCheckingDevices(true);

      const { stream, permissions } = await requestDeviceAccess({ audio, video });
      setPermissionState((previousState) => ({
        ...previousState,
        ...permissions,
      }));

      if (video) {
        setPreviewStream((previousStream) => {
          stopMediaStream(previousStream);
          return stream;
        });
      } else {
        stopMediaStream(stream);
      }

      return true;
    } catch (error) {
      console.error("Device access request failed:", error);
      await syncPermissions();
      setDeviceError(
        getPermissionErrorMessage(
          error,
          audio && video ? "microphone and camera" : audio ? "microphone" : "camera"
        )
      );
      return false;
    } finally {
      setIsCheckingDevices(false);
    }
  };

  const ensureSelectedDevicesReady = async () => {
    const requiresMicrophone = micOn && permissionState.microphone !== "granted";
    const requiresCamera = videoOn && permissionState.camera !== "granted";

    if (!requiresMicrophone && !requiresCamera) {
      if (videoOn && !previewStream && permissionState.camera === "granted") {
        await requestPermissions({ video: true });
      }
      return true;
    }

    return requestPermissions({
      audio: requiresMicrophone,
      video: requiresCamera,
    });
  };

  const handleToggleMic = async () => {
    if (micOn) {
      setMicOn(false);
      return;
    }

    if (permissionState.microphone === "granted") {
      setMicOn(true);
      return;
    }

    const granted = await requestPermissions({ audio: true });
    if (granted) {
      setMicOn(true);
    }
  };

  const handleToggleCamera = async () => {
    if (videoOn) {
      setVideoOn(false);
      setPreviewStream((previousStream) => {
        stopMediaStream(previousStream);
        return null;
      });
      return;
    }

    if (permissionState.camera === "granted") {
      setVideoOn(true);
      await requestPermissions({ video: true });
      return;
    }

    const granted = await requestPermissions({ video: true });
    if (granted) {
      setVideoOn(true);
    }
  };

  const goToMeetingRoom = (res) => {
    stopMediaStream(previewStreamRef.current);
    navigate(`/meeting/${code}`, {
      state: {
        token: res.token,
        role: res.role,
        isOwner: Boolean(res.isOwner),
        currentHostId: res.currentHostId,
        serverUrl: res.serverUrl,
        settings: res.settings,
        micOn: mediaPreferenceRef.current.micOn,
        camOn: mediaPreferenceRef.current.videoOn,
      },
    });
  };

  const handleAskToJoin = async () => {
    const devicesReady = await ensureSelectedDevicesReady();
    if (!devicesReady) {
      return;
    }

    setJoinState("ASKING");
    setErrorMsg(null);

    try {
      const response = await meetingService.joinMeeting(code);

      if (response.status === "APPROVED") {
        goToMeetingRoom(response);
      } else if (response.status === "PENDING") {
        setJoinState("WAITING");
        setWaitingMessage(response.message);

        if (!stompClient || !stompClient.active) {
          connectWebSocket();
        }
      }
    } catch (error) {
      console.error("Unable to join meeting:", error);
      setJoinState("IDLE");
      setErrorMsg(
        error.response?.data?.message ||
          "An error occurred while trying to join the meeting. Please try again."
      );
    }
  };

  const connectWebSocket = () => {
    const client = new Client({
      brokerURL: getWebSocketUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/meeting/${code}/user/${user.userId}`, (message) => {
          const res = JSON.parse(message.body);

          if (res.status === "APPROVED") {
            goToMeetingRoom(res);
          } else if (res.status === "REJECTED") {
            setJoinState("IDLE");
            setErrorMsg("Your request to join the meeting was rejected by the host.");
            client.deactivate();
          }
        });

        client.subscribe(`/topic/meeting/${code}/waiting-room`, async (msg) => {
          if (msg.body === "HOST_JOINED" || msg.body === "SETTINGS_CHANGED") {
            if (msg.body === "HOST_JOINED") {
              setWaitingMessage("The meeting has started. Please wait for the host to let you in.");
            }

            try {
              const checkRes = await meetingService.joinMeeting(code);
              if (checkRes.status === "APPROVED") {
                goToMeetingRoom(checkRes);
              }
            } catch (error) {
              console.error("Unable to re-check meeting status:", error);
            }
          }
        });
      },
      onStompError: (frame) => {
        console.error("Broker error:", frame.headers["message"]);
      },
    });

    client.activate();
    setStompClient(client);
  };

  const permissionBadgeClass = (state) => {
    if (state === "granted") return "bg-green-500/10 text-green-300 border-green-500/20";
    if (state === "denied") return "bg-red-500/10 text-red-300 border-red-500/20";
    return "bg-yellow-500/10 text-yellow-300 border-yellow-500/20";
  };

  const permissionBadgeText = (state) => {
    if (state === "granted") return "Allowed";
    if (state === "denied") return "Blocked";
    return "Needs permission";
  };

  const renderActionButton = () => {
    if (joinState === "ASKING") {
      return (
        <button
          disabled
          className="w-full h-16 bg-blue-900 text-blue-200 text-xl font-black rounded-2xl flex items-center justify-center gap-3 cursor-not-allowed transition-all"
        >
          <div className="size-5 border-2 border-blue-200/30 border-t-blue-200 rounded-full animate-spin"></div>
          {isHostSetup ? "Preparing meeting..." : "Sending request..."}
        </button>
      );
    }

    if (joinState === "WAITING") {
      return (
        <div className="flex flex-col gap-4">
          <div className="bg-blue-500/10 border border-blue-500/20 text-blue-300 text-sm py-3 px-4 rounded-xl flex items-start gap-3 text-left animate-fade-in">
            <span className="material-symbols-outlined mt-0.5 text-lg">info</span>
            <span className="leading-relaxed">{waitingMessage}</span>
          </div>

          <button
            disabled
            className="w-full h-16 bg-slate-800 text-gray-400 text-lg font-bold rounded-2xl flex items-center justify-center gap-3 cursor-not-allowed transition-all shadow-inner"
          >
            <div className="size-5 border-2 border-gray-400/30 border-t-gray-400 rounded-full animate-spin"></div>
            Waiting for host...
          </button>
        </div>
      );
    }

    return (
      <button
        onClick={handleAskToJoin}
        className="w-full h-16 bg-primary hover:bg-blue-600 text-white text-xl font-black rounded-2xl shadow-xl transition-all active:scale-95"
      >
        {isHostSetup ? "Start meeting" : "Join now"}
      </button>
    );
  };

  return (
    <div className="min-h-screen flex flex-col">
      <header className="h-20 flex items-center justify-between px-6 border-b border-white/5">
        <div className="flex items-center gap-2">
          <div className="size-8 bg-primary rounded-lg flex items-center justify-center text-white">
            <span className="material-symbols-outlined text-xl">videocam</span>
          </div>
          <span className="text-lg font-bold">PTIT-Meet</span>
        </div>
      </header>

      <main className="flex-grow flex items-center justify-center p-6">
        <div className="max-w-5xl w-full grid lg:grid-cols-2 gap-12 items-center">
          <div className="space-y-8">
            <div className="relative aspect-video bg-surface rounded-2xl overflow-hidden shadow-2xl border border-white/10 group">
              {isCheckingDevices && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-sm z-20">
                  <div className="size-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4"></div>
                  <p className="text-sm font-bold text-gray-300">Checking camera & mic...</p>
                </div>
              )}

              {videoOn && previewStream ? (
                <video
                  ref={videoRef}
                  autoPlay
                  muted
                  playsInline
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-950 to-black">
                  <div className="size-32 rounded-full bg-primary/20 flex items-center justify-center text-primary text-5xl font-black">
                    {user?.fullName?.charAt(0) || "U"}
                  </div>
                </div>
              )}

              <div className="absolute top-4 right-4 flex items-center gap-2 bg-black/40 backdrop-blur px-3 py-1 rounded-full border border-white/10">
                <div className={`w-2 h-2 rounded-full ${videoOn && previewStream ? "bg-green-500 animate-pulse" : "bg-gray-500"}`}></div>
                <span className="text-[10px] font-bold uppercase tracking-wider">
                  {videoOn && previewStream ? "Preview ready" : "Camera off"}
                </span>
              </div>

              <div className="absolute bottom-6 left-0 right-0 flex justify-center gap-4 z-10">
                <button
                  onClick={handleToggleMic}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    micOn ? "bg-white/10 hover:bg-white/20 text-white" : "bg-red-500 text-white"
                  }`}
                >
                  <span className="material-symbols-outlined">{micOn ? "mic" : "mic_off"}</span>
                </button>
                <button
                  onClick={handleToggleCamera}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    videoOn ? "bg-primary hover:bg-blue-600 text-white" : "bg-red-500 text-white"
                  }`}
                >
                  <span className="material-symbols-outlined">{videoOn ? "videocam" : "videocam_off"}</span>
                </button>
              </div>
            </div>
          </div>

          <div className="space-y-8 text-center lg:text-left">
            <div>
              <h1 className="text-4xl font-black mb-4">
                {isHostSetup ? "Set up before you go live" : "Ready to join?"}
              </h1>
              <p className="text-gray-400">
                {isHostSetup
                  ? "Check your camera and microphone before entering so the meeting starts smoothly."
                  : "Allow camera and microphone access before entering so you can join right away."}
              </p>
            </div>

            <div className="rounded-3xl border border-white/10 bg-white/5 p-5 text-left">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-bold text-white">Browser permissions</p>
                  <p className="mt-1 text-xs text-gray-400">
                    Use the button below to show the browser permission prompt for your camera and microphone.
                  </p>
                </div>
                <button
                  onClick={() => requestPermissions({ audio: true, video: true })}
                  className="rounded-2xl bg-primary px-4 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-600"
                >
                  Allow mic & cam
                </button>
              </div>

              <div className="mt-4 flex flex-wrap gap-3">
                <div className={`rounded-full border px-3 py-2 text-xs font-bold ${permissionBadgeClass(permissionState.microphone)}`}>
                  Microphone: {permissionBadgeText(permissionState.microphone)}
                </div>
                <div className={`rounded-full border px-3 py-2 text-xs font-bold ${permissionBadgeClass(permissionState.camera)}`}>
                  Camera: {permissionBadgeText(permissionState.camera)}
                </div>
              </div>

              {deviceError && (
                <div className="mt-4 rounded-2xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                  {deviceError}
                </div>
              )}
            </div>

            <div className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-bold text-gray-500 block">Your Display Name</label>
                <input
                  type="text"
                  value={user?.fullName || ""}
                  readOnly
                  className="w-full h-14 px-5 rounded-2xl bg-surface border border-white/10 text-lg font-bold"
                />
              </div>

              {errorMsg && (
                <div className="bg-red-500/10 border border-red-500/30 text-red-500 text-sm py-3 px-4 rounded-xl flex items-center gap-2 text-left">
                  <span className="material-symbols-outlined">error</span>
                  <span>{errorMsg}</span>
                </div>
              )}

              <div className="space-y-4">
                {renderActionButton()}

                <button
                  onClick={() => navigate("/")}
                  className="w-full h-14 bg-transparent hover:bg-slate-200 dark:hover:bg-white/5 text-gray-500 dark:text-gray-400 font-bold rounded-2xl transition-all"
                >
                  Cancel
                </button>
              </div>
            </div>

            <div className="flex flex-col gap-3 p-4 bg-white/5 rounded-2xl border border-white/5 text-sm text-gray-500">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary">headphones</span>
                <span>Microphone status: {permissionBadgeText(permissionState.microphone)}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary">photo_camera</span>
                <span>Camera status: {permissionBadgeText(permissionState.camera)}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default WaitingRoomPage;
