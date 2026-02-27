import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { Client } from "@stomp/stompjs";
import { meetingService } from "../services/meetingService";

const WaitingRoomPage = () => {
  const { code } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [micOn, setMicOn] = useState(true);
  const [videoOn, setVideoOn] = useState(true);
  const [isCheckingDevices, setIsCheckingDevices] = useState(true);

  const [joinState, setJoinState] = useState("IDLE"); // IDLE (Mới vào) -> ASKING (Đang gọi API) -> WAITING (Đang chờ duyệt)
  const [errorMsg, setErrorMsg] = useState(null);

  const [waitingMessage, setWaitingMessage] = useState("Joining meeting...");
  const [isWaiting, setIsWaiting] = useState(false);

  const [stompClient, setStompClient] = useState(null);

  //Mock Check Devices Call
  useEffect(() => {
    const timer = setTimeout(() => setIsCheckingDevices(false), 1500);
    return () => clearTimeout(timer);
  }, []);

  const handleAskToJoin = async () => {
    if (!isWaiting) {
      setJoinState("ASKING");
      setErrorMsg(null);
    }

    try {
      const response = await meetingService.joinMeeting(code);

      if (response.status === "APPROVED") {
        navigate(`/meeting/${code}`, {
          state: {
            token: response.token,
            role: response.role,
            serverUrl: response.serverUrl,
            micOn: micOn,
            videoOn: videoOn,
          },
        });
      } else if (response.status === "PENDING") {
        setJoinState("WAITING");
        setWaitingMessage(response.message);
        setIsWaiting(true);

        if (!stompClient || !stompClient.active) {
          connectWebSocket();
        }
      }
    } catch (error) {
      console.error("Lỗi khi gọi API join:", error);
      setJoinState("IDLE");
      setErrorMsg(
        error.response?.data?.message || "An error occurred while trying to join the meeting. Please try again."
      );
    }
  };

  const connectWebSocket = () => {
    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(
          `/topic/meeting/${code}/user/${user.userId}`,
          (message) => {
            const res = JSON.parse(message.body);

            if (res.status === "APPROVED") {
              navigate(`/meeting/${code}`, {
                state: {
                  token: res.token,
                  role: res.role,
                  serverUrl: res.serverUrl,
                  micOn: micOn,
                  videoOn: videoOn,
                },
              });
            } else if (res.status === "REJECTED") {
              setJoinState("IDLE");
              setIsWaiting(false);
              setErrorMsg("Your request to join the meeting was rejected by the host.");
              client.deactivate();
            }
          }
        );

        client.subscribe(
          `/topic/meeting/${code}/waiting-room`,
          async (msg) => {
            if (msg.body === "HOST_JOINED") {
              setWaitingMessage("The meeting has started. Please wait for the host to let you in.");
              
              try {
                const checkRes = await meetingService.joinMeeting(code);
                if (checkRes.status === "APPROVED") {
                  navigate(`/meeting/${code}`, {
                    state: {
                      token: checkRes.token,
                      role: checkRes.role,
                      serverUrl: checkRes.serverUrl,
                      micOn: micOn,
                      videoOn: videoOn,
                    },
                  });
                }
              } catch (err) {
                console.error("Lỗi khi re-check trạng thái phòng:", err);
              }
            }
          }
        );
      },
      onStompError: (frame) => {
        console.error("Lỗi Broker: ", frame.headers["message"]);
      },
    });

    client.activate();
    setStompClient(client);
  };

  useEffect(() => {
    return () => {
      if (stompClient && stompClient.active) stompClient.deactivate();
    };
  }, [stompClient]);

  const renderActionButton = () => {
    if (joinState === "ASKING") {
      return (
        <button
          disabled
          className="w-full h-16 bg-blue-900 text-blue-200 text-xl font-black rounded-2xl flex items-center justify-center gap-3 cursor-not-allowed transition-all"
        >
          <div className="size-5 border-2 border-blue-200/30 border-t-blue-200 rounded-full animate-spin"></div>
          Sending request...
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
        Join now
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
        <div className="flex items-center gap-4">
          <button className="p-2 rounded-full hover:bg-white/5 text-gray-400">
            <span className="material-symbols-outlined">help</span>
          </button>
          <button className="p-2 rounded-full hover:bg-white/5 text-gray-400">
            <span className="material-symbols-outlined">settings</span>
          </button>
        </div>
      </header>

      <main className="flex-grow flex items-center justify-center p-6">
        <div className="max-w-5xl w-full grid lg:grid-cols-2 gap-12 items-center">
          <div className="space-y-8">
            <div className="relative aspect-video bg-surface rounded-2xl overflow-hidden shadow-2xl border border-white/10 group">
              {isCheckingDevices ? (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-sm z-20">
                  <div className="size-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4"></div>
                  <p className="text-sm font-bold text-gray-300">
                    Checking camera & mic...
                  </p>
                </div>
              ) : (
                <img
                  src="https://picsum.photos/1280/720?random=2"
                  alt="Camera Preview"
                  className={`w-full h-full object-cover transition-opacity duration-500 ${
                    videoOn ? "opacity-100" : "opacity-0"
                  }`}
                />
              )}

              {!videoOn && (
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="size-32 rounded-full bg-primary/20 flex items-center justify-center text-primary text-5xl font-black">
                    {user?.fullName?.charAt(0) || "U"}
                  </div>
                </div>
              )}
              <div className="absolute top-4 right-4 flex items-center gap-2 bg-black/40 backdrop-blur px-3 py-1 rounded-full border border-white/10">
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                <span className="text-[10px] font-bold uppercase tracking-wider">
                  HD Live
                </span>
              </div>
              <div className="absolute bottom-6 left-0 right-0 flex justify-center gap-4 z-10">
                <button
                  onClick={() => setMicOn(!micOn)}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    micOn
                      ? "bg-white/10 hover:bg-white/20 text-white"
                      : "bg-red-500 text-white"
                  }`}
                >
                  <span className="material-symbols-outlined">
                    {micOn ? "mic" : "mic_off"}
                  </span>
                </button>
                <button
                  onClick={() => setVideoOn(!videoOn)}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    videoOn
                      ? "bg-primary hover:bg-blue-600 text-white"
                      : "bg-red-500 text-white"
                  }`}
                >
                  <span className="material-symbols-outlined">
                    {videoOn ? "videocam" : "videocam_off"}
                  </span>
                </button>
                <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center">
                  <span className="material-symbols-outlined">
                    auto_awesome
                  </span>
                </button>
              </div>
            </div>
          </div>

          <div className="space-y-8 text-center lg:text-left">
            <div>
              <h1 className="text-4xl font-black mb-4">Ready to join?</h1>
              <p className="text-gray-400">
                2 other people are already in this call.
              </p>
            </div>

            <div className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-bold text-gray-500 block">
                  Your Display Name
                </label>
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
                <span className="material-symbols-outlined text-primary">
                  headphones
                </span>
                <span>
                  Mic:{" "}
                  {isCheckingDevices
                    ? "Detecting..."
                    : "MacBook Pro Microphone (System)"}
                </span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default WaitingRoomPage;
