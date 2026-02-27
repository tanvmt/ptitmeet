import React, { useState, useEffect, useRef } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { meetingService } from "../services/meetingService";
import { Client } from "@stomp/stompjs";

const MeetingPage = () => {
  const { code } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const chatEndRef = useRef(null);

  // Nhận dữ liệu truyền từ Dashboard qua navigate state
  const joinData = location.state || {};
  const isHost = joinData.role === "HOST";

  // States: Đồng bộ trạng thái mic/cam từ Dashboard
  const [isMuted, setIsMuted] = useState(!(joinData.micOn ?? true));
  const [isVideoOff, setIsVideoOff] = useState(!(joinData.camOn ?? true));

  // States: Người tham gia (khởi tạo mảng rỗng thay vì dữ liệu cứng)
  const [participants, setParticipants] = useState([]);

  // States: UI & Khác
  const [activeTab, setActiveTab] = useState("chat");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isRecording, setIsRecording] = useState(true);
  const [timer, setTimer] = useState(0);

  // States: Chat
  const [newMessage, setNewMessage] = useState("");
  const [chatMessages, setChatMessages] = useState([]); // Chuyển thành mảng rỗng ban đầu

  // States: Phòng chờ
  const [waitingList, setWaitingList] = useState([]);
  const [isLoadingWaiting, setIsLoadingWaiting] = useState(false);

  // Effect: Khởi tạo chính mình (Me) vào danh sách participants khi vào phòng
  useEffect(() => {
    if (user) {
      setParticipants([
        {
          id: user.id || "me",
          name: `${user.fullName} (You)`,
          avatar: user.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=random`,
          isMe: true,
          isHost: isHost,
          isSpeaking: false,
          isMuted: isMuted,
          isVideoOff: isVideoOff,
        },
      ]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, isHost]);

  // Effect: Đồng bộ state isMuted / isVideoOff của chính mình vào mảng participants khi nhấn nút
  useEffect(() => {
    setParticipants((prev) =>
        prev.map((p) =>
            p.isMe ? { ...p, isMuted: isMuted, isVideoOff: isVideoOff } : p
        )
    );
  }, [isMuted, isVideoOff]);

  useEffect(() => {
    if (isHost) {
      fetchWaitingList();
    }
  }, [isHost]);

  useEffect(() => {
    if (isHost && sidebarOpen && activeTab === "people") {
      fetchWaitingList();
    }
  }, [sidebarOpen, activeTab, isHost]);

  // Kết nối Web Socket để nghe sự kiện phòng chờ (dành cho Host)
  useEffect(() => {
    if (!isHost || !code) return;

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("Host đã kết nối kênh Admin!");

        client.subscribe(`/topic/meeting/${code}/admin`, (message) => {
          console.log("Có người vừa xin vào phòng chờ!");
          fetchWaitingList();
        });
      },
      onStompError: (frame) => {
        console.error("Lỗi Broker Host:", frame.headers["message"]);
      },
    });

    client.activate();

    return () => {
      if (client.active) client.deactivate();
    };
  }, [isHost, code]);

  const fetchWaitingList = async () => {
    try {
      setIsLoadingWaiting(true);
      const data = await meetingService.getWaitingList(code);
      setWaitingList(data);
    } catch (error) {
      console.error("Lỗi lấy danh sách chờ:", error);
    } finally {
      setIsLoadingWaiting(false);
    }
  };

  const handleApproval = async (participantId, action) => {
    try {
      await meetingService.processApproval(code, participantId, action);
      setWaitingList((prev) =>
          prev.filter((p) => p.participantId !== participantId)
      );
    } catch (error) {
      alert("Lỗi xử lý duyệt: " + (error.response?.data?.message || ""));
    }
  };

  // Tự động cuộn xuống khi có tin nhắn mới
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatMessages, sidebarOpen]);

  useEffect(() => {
    const interval = setInterval(() => setTimer((t) => t + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  const sendMessage = (e) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    setChatMessages([
      ...chatMessages,
      {
        id: Date.now(),
        user: `${user?.fullName || "You"}`,
        text: newMessage,
        time: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
        isMe: true,
      },
    ]);
    setNewMessage("");
  };

  const formatTime = (seconds) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h > 0 ? h + ":" : ""}${m.toString().padStart(2, "0")}:${s
        .toString()
        .padStart(2, "0")}`;
  };

  return (
      <div className="h-screen flex flex-col bg-background overflow-hidden text-white font-sans">
        {/* Header */}
        <header className="h-16 flex items-center justify-between px-6 bg-background/80 backdrop-blur-md z-30 shrink-0 border-b border-white/5">
          <div className="flex items-center gap-4">
            <div className="flex flex-col">
              <h1 className="text-sm font-bold leading-none mb-1">
                Weekly Product Sync
              </h1>
              <div className="flex items-center gap-2 text-[10px] text-gray-500 font-bold uppercase tracking-widest">
                {/* Cập nhật mã phòng động từ params */}
                <span>{code}</span>
                {isRecording && (
                    <div className="flex items-center gap-1 text-red-500">
                      <div className="size-1.5 rounded-full bg-red-500 animate-pulse"></div>
                      REC {formatTime(timer)}
                    </div>
                )}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden md:flex items-center gap-2 bg-surface px-4 py-1.5 rounded-full border border-white/5">
            <span className="material-symbols-outlined text-green-500 text-[18px]">
              lock
            </span>
              <span className="text-[10px] font-bold text-gray-400 uppercase">
              Encrypted
            </span>
            </div>
            <button className="p-2 rounded-full hover:bg-white/5 text-gray-400">
              <span className="material-symbols-outlined">info</span>
            </button>
          </div>
        </header>

        <div className="flex-grow flex relative">
          {/* Main Grid Area */}
          <main
              className={`flex-grow p-4 transition-all duration-300 ${
                  sidebarOpen ? "lg:mr-80" : ""
              }`}
          >
            <div className="h-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 content-center max-w-7xl mx-auto">
              {participants.map((p) => (
                  <div
                      key={p.id}
                      className={`group relative aspect-video bg-surface rounded-2xl overflow-hidden border-2 transition-all ${
                          p.isSpeaking
                              ? "border-primary shadow-lg shadow-primary/20"
                              : "border-transparent"
                      }`}
                  >
                    {!p.isVideoOff ? (
                        <img
                            src={p.avatar}
                            alt={p.name}
                            className="w-full h-full object-cover"
                        />
                    ) : (
                        <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-800 to-gray-900">
                          <div className="size-20 rounded-full bg-surface border border-white/10 flex items-center justify-center text-white text-2xl font-black shadow-2xl">
                            {p.name.charAt(0)}
                          </div>
                        </div>
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60"></div>
                    <div className="absolute bottom-4 left-4 flex items-center gap-2">
                  <span className="text-xs font-bold text-white tracking-wide truncate max-w-[120px]">
                    {p.name}
                  </span>
                      {p.isMuted && (
                          <span className="material-symbols-outlined text-red-500 text-[16px]">
                      mic_off
                    </span>
                      )}
                      {p.isMe && (
                          <span className="bg-primary px-1.5 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">
                      Me
                    </span>
                      )}
                    </div>
                    {p.isSpeaking && (
                        <div className="absolute bottom-4 right-4 flex items-end gap-0.5 h-3">
                          <div className="w-1 bg-primary rounded-full animate-[pulse_1s_infinite] h-2"></div>
                          <div className="w-1 bg-primary rounded-full animate-[pulse_1.2s_infinite] h-3"></div>
                          <div className="w-1 bg-primary rounded-full animate-[pulse_0.8s_infinite] h-1.5"></div>
                        </div>
                    )}
                  </div>
              ))}
            </div>
          </main>

          {/* Sidebar */}
          <aside
              className={`fixed top-16 right-0 bottom-24 w-80 bg-surface border-l border-white/5 z-20 transition-transform duration-300 shadow-2xl ${
                  sidebarOpen ? "translate-x-0" : "translate-x-full"
              }`}
          >
            <div className="flex flex-col h-full">
              {/* Tabs Navigation */}
              <div className="flex p-2 gap-1 border-b border-white/5 bg-background/20">
                <button
                    onClick={() => setActiveTab("chat")}
                    className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${
                        activeTab === "chat"
                            ? "bg-primary text-white shadow-lg"
                            : "text-gray-500 hover:bg-white/5"
                    }`}
                >
                  In-call Messages
                </button>
                <button
                    onClick={() => setActiveTab("people")}
                    className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${
                        activeTab === "people"
                            ? "bg-primary text-white shadow-lg"
                            : "text-gray-500 hover:bg-white/5"
                    }`}
                >
                  People ({participants.length})
                </button>
              </div>

              {/* Chat Content */}
              {activeTab === "chat" && (
                  <>
                    <div className="flex-grow overflow-y-auto p-4 space-y-4 no-scrollbar">
                      <p className="text-[10px] text-center text-gray-500 font-bold uppercase tracking-widest bg-white/5 py-2 rounded-lg">
                        Messages are visible only to people in the call
                      </p>
                      {chatMessages.map((msg) => (
                          <div
                              key={msg.id}
                              className={`flex flex-col ${
                                  msg.isMe ? "items-end" : "items-start"
                              }`}
                          >
                            <div className="flex items-center gap-2 mb-1 px-1">
                              {!msg.isMe && (
                                  <span className="text-[10px] font-black text-gray-400">
                            {msg.user}
                          </span>
                              )}
                              <span className="text-[9px] text-gray-600">
                          {msg.time}
                        </span>
                            </div>
                            <div
                                className={`max-w-[85%] p-3 text-sm leading-relaxed ${
                                    msg.isMe
                                        ? "bg-primary text-white rounded-2xl rounded-tr-none shadow-md shadow-primary/10"
                                        : "bg-white/5 text-gray-200 rounded-2xl rounded-tl-none border border-white/5"
                                }`}
                            >
                              {msg.text}
                            </div>
                          </div>
                      ))}
                      <div ref={chatEndRef} />
                    </div>
                    <form
                        onSubmit={sendMessage}
                        className="p-4 border-t border-white/5 bg-background/40"
                    >
                      <div className="relative">
                        <input
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            placeholder="Send a message..."
                            className="w-full bg-surface border border-white/10 rounded-xl py-3 pl-4 pr-12 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
                        />
                        <button
                            type="submit"
                            className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 text-primary hover:bg-primary/10 rounded-lg transition-colors"
                        >
                          <span className="material-symbols-outlined">send</span>
                        </button>
                      </div>
                    </form>
                  </>
              )}

              {/* People Content */}
              {activeTab === "people" && (
                  <div className="flex-grow flex flex-col overflow-y-auto no-scrollbar">
                    <div className="p-4 border-b border-white/5">
                      <button className="w-full py-2.5 rounded-xl border border-primary/30 text-primary text-xs font-bold hover:bg-primary/5 transition-colors flex items-center justify-center gap-2">
                    <span className="material-symbols-outlined text-sm">
                      person_add
                    </span>
                        Invite someone
                      </button>
                    </div>

                    {/* WAITING ROOM (Chỉ hiển thị với HOST) */}
                    {isHost && waitingList.length > 0 && (
                        <div className="mb-2">
                          <div className="px-4 py-2 bg-red-500/10 text-red-400 text-[10px] font-bold uppercase tracking-widest flex justify-between items-center border-y border-red-500/10">
                            <span>Waiting Room ({waitingList.length})</span>
                            <button
                                onClick={fetchWaitingList}
                                className="hover:text-red-300"
                            >
                        <span className="material-symbols-outlined text-sm">
                          refresh
                        </span>
                            </button>
                          </div>
                          <div className="p-2 space-y-2">
                            {isLoadingWaiting ? (
                                <div className="p-4 text-center text-sm text-gray-500">
                                  Đang tải...
                                </div>
                            ) : (
                                waitingList.map((p) => (
                                    <div
                                        key={p.participantId}
                                        className="bg-white/5 p-2 rounded-xl border border-white/10 flex flex-col gap-2"
                                    >
                                      <div className="flex items-center gap-2">
                                        <div className="size-8 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold">
                                          {p.displayName?.charAt(0) || "U"}
                                        </div>
                                        <div className="flex flex-col flex-1 overflow-hidden">
                                <span className="text-sm font-semibold truncate">
                                  {p.displayName}
                                </span>
                                          <span className="text-[10px] text-gray-500 truncate">
                                  {p.email}
                                </span>
                                        </div>
                                      </div>
                                      <div className="flex gap-2">
                                        <button
                                            onClick={() =>
                                                handleApproval(p.participantId, "APPROVED")
                                            }
                                            className="flex-1 bg-green-500/20 hover:bg-green-500 text-green-500 hover:text-white py-1.5 rounded-lg text-xs font-bold transition-all"
                                        >
                                          Admit
                                        </button>
                                        <button
                                            onClick={() =>
                                                handleApproval(p.participantId, "REJECTED")
                                            }
                                            className="flex-1 bg-red-500/20 hover:bg-red-500 text-red-500 hover:text-white py-1.5 rounded-lg text-xs font-bold transition-all"
                                        >
                                          Deny
                                        </button>
                                      </div>
                                    </div>
                                ))
                            )}
                          </div>
                        </div>
                    )}

                    {/* IN-CALL PARTICIPANTS */}
                    <div>
                      <div className="px-4 py-2 bg-white/5 text-gray-400 text-[10px] font-bold uppercase tracking-widest border-y border-white/5">
                        In Call ({participants.length})
                      </div>
                      <div className="p-2 space-y-1">
                        {participants.map((p) => (
                            <div
                                key={p.id}
                                className="flex items-center justify-between p-2 rounded-xl hover:bg-white/5 transition-colors group"
                            >
                              <div className="flex items-center gap-3">
                                <div className="relative">
                                  <img
                                      src={p.avatar}
                                      className="size-8 rounded-full border border-white/10"
                                      alt=""
                                  />
                                  {p.isSpeaking && (
                                      <div className="absolute -bottom-0.5 -right-0.5 size-2.5 bg-green-500 rounded-full border-2 border-surface"></div>
                                  )}
                                </div>
                                <div className="flex flex-col">
                            <span className="text-sm font-semibold truncate max-w-[120px]">
                              {p.name}
                            </span>
                                  {p.isHost && (
                                      <span className="text-[9px] text-primary font-bold uppercase">
                                Meeting Host
                              </span>
                                  )}
                                </div>
                              </div>
                              <div className="flex items-center gap-1 opacity-60 group-hover:opacity-100 transition-opacity">
                          <span
                              className={`material-symbols-outlined text-[18px] ${
                                  p.isMuted ? "text-red-500" : "text-gray-400"
                              }`}
                          >
                            {p.isMuted ? "mic_off" : "mic"}
                          </span>
                                <button className="material-symbols-outlined text-[18px] text-gray-400 hover:text-white">
                                  more_vert
                                </button>
                              </div>
                            </div>
                        ))}
                      </div>
                    </div>

                    {isHost && (
                        <div className="p-4 border-t border-white/5 mt-auto">
                          <button className="w-full py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold transition-colors">
                            Mute All Participants
                          </button>
                        </div>
                    )}
                  </div>
              )}
            </div>
          </aside>
        </div>

        {/* Control Bar */}
        <footer className="h-24 flex items-center justify-center px-6 relative z-40 bg-background shrink-0 border-t border-white/5">
          <div className="flex items-center gap-3 bg-surface/90 backdrop-blur-xl p-2 rounded-full border border-white/10 shadow-2xl">
            <button
                onClick={() => setIsMuted(!isMuted)}
                className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    !isMuted
                        ? "bg-white/10 hover:bg-white/20 text-white"
                        : "bg-red-500 text-white shadow-lg shadow-red-500/20"
                }`}
            >
            <span className="material-symbols-outlined">
              {!isMuted ? "mic" : "mic_off"}
            </span>
            </button>
            <button
                onClick={() => setIsVideoOff(!isVideoOff)}
                className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    !isVideoOff
                        ? "bg-white/10 hover:bg-white/20 text-white"
                        : "bg-red-500 text-white shadow-lg shadow-red-500/20"
                }`}
            >
            <span className="material-symbols-outlined">
              {!isVideoOff ? "videocam" : "videocam_off"}
            </span>
            </button>
            <div className="w-px h-8 bg-white/10 mx-1"></div>
            <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">
              present_to_all
            </span>
            </button>
            <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">
              sentiment_satisfied
            </span>
            </button>
            <button
                onClick={() => {
                  if (sidebarOpen && activeTab === "chat") setSidebarOpen(false);
                  else {
                    setSidebarOpen(true);
                    setActiveTab("chat");
                  }
                }}
                className={`size-12 rounded-full flex items-center justify-center transition-all ${
                    sidebarOpen && activeTab === "chat"
                        ? "bg-primary text-white shadow-lg shadow-primary/20"
                        : "bg-white/10 hover:bg-white/20 text-white"
                }`}
            >
            <span className="material-symbols-outlined text-[22px]">
              chat_bubble
            </span>
            </button>
            <button
                onClick={() => {
                  if (sidebarOpen && activeTab === "people") setSidebarOpen(false);
                  else {
                    setSidebarOpen(true);
                    setActiveTab("people");
                  }
                }}
                className={`relative size-12 rounded-full flex items-center justify-center transition-all ${
                    sidebarOpen && activeTab === "people"
                        ? "bg-primary text-white shadow-lg shadow-primary/20"
                        : "bg-white/10 hover:bg-white/20 text-white"
                }`}
            >
              <span className="material-symbols-outlined text-[22px]">group</span>
              {isHost && waitingList.length > 0 && (
                  <span className="absolute 0 top-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white font-bold border border-background animate-bounce">
                {waitingList.length}
              </span>
              )}
            </button>
            <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">
              more_vert
            </span>
            </button>
            <div className="w-px h-8 bg-white/10 mx-1"></div>
            <button
                onClick={() => navigate("/summary")} // Hoặc quay về /dashboard
                className="px-8 py-3 rounded-full bg-red-500 hover:bg-red-600 text-white font-black text-sm shadow-xl shadow-red-500/20 transition-all active:scale-95"
            >
              Leave Call
            </button>
          </div>
        </footer>
      </div>
  );
};

export default MeetingPage;