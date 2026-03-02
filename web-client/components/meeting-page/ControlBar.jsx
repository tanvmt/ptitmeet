import React from "react";
import { useNavigate } from "react-router-dom";
import { useRoomContext, useLocalParticipant } from "@livekit/components-react";

const ControlBar = ({
                        sidebarOpen, setSidebarOpen, activeTab, setActiveTab, waitingCount, isHost
                    }) => {
    const navigate = useNavigate();
    const room = useRoomContext(); // Lấy context phòng của LiveKit

    // LẤY QUYỀN ĐIỀU KHIỂN PHẦN CỨNG TỪ LIVEKIT
    const { localParticipant, isMicrophoneEnabled, isCameraEnabled, isScreenShareEnabled } = useLocalParticipant();

    // 1. Hàm bật/tắt Mic phần cứng
    const toggleMic = async () => {
        if (localParticipant) {
            await localParticipant.setMicrophoneEnabled(!isMicrophoneEnabled);
        }
    };

    // 2. Hàm bật/tắt Cam phần cứng
    const toggleCam = async () => {
        if (localParticipant) {
            await localParticipant.setCameraEnabled(!isCameraEnabled);
        }
    };

    // 3. Hàm bật/tắt Chia sẻ màn hình
    const toggleScreenShare = async () => {
        if (localParticipant) {
            await localParticipant.setScreenShareEnabled(!isScreenShareEnabled);
        }
    };

    const handleDisconnect = () => {
        room.disconnect(); // Cắt kết nối LiveKit
        navigate("/summary"); // Điều hướng về trang bạn muốn
    };

    return (
        <footer className="h-24 w-full flex items-center justify-center px-6 relative z-40 bg-background shrink-0 border-t border-white/5">
            <div className="flex items-center gap-3 bg-surface/90 backdrop-blur-xl p-2 rounded-full border border-white/10 shadow-2xl">

                {/* NÚT MIC */}
                <button
                    onClick={toggleMic}
                    className={`size-12 rounded-full flex items-center justify-center transition-all ${
                        isMicrophoneEnabled ? "bg-white/10 hover:bg-white/20 text-white" : "bg-red-500 text-white shadow-lg shadow-red-500/20"
                    }`}
                >
                    <span className="material-symbols-outlined">{isMicrophoneEnabled ? "mic" : "mic_off"}</span>
                </button>

                {/* NÚT CAM */}
                <button
                    onClick={toggleCam}
                    className={`size-12 rounded-full flex items-center justify-center transition-all ${
                        isCameraEnabled ? "bg-white/10 hover:bg-white/20 text-white" : "bg-red-500 text-white shadow-lg shadow-red-500/20"
                    }`}
                >
                    <span className="material-symbols-outlined">{isCameraEnabled ? "videocam" : "videocam_off"}</span>
                </button>

                <div className="w-px h-8 bg-white/10 mx-1"></div>

                {/* NÚT SHARE SCREEN */}
                <button
                    onClick={toggleScreenShare}
                    className={`size-12 rounded-full flex items-center justify-center transition-all ${
                        isScreenShareEnabled ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-white/10 hover:bg-white/20 text-white"
                    }`}
                >
          <span className="material-symbols-outlined text-[22px]">
            {isScreenShareEnabled ? "stop_screen_share" : "present_to_all"}
          </span>
                </button>

                <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
                    <span className="material-symbols-outlined text-[22px]">sentiment_satisfied</span>
                </button>

                <button
                    onClick={() => { setSidebarOpen(sidebarOpen && activeTab === "chat" ? false : true); setActiveTab("chat"); }}
                    className={`size-12 rounded-full flex items-center justify-center transition-all ${
                        sidebarOpen && activeTab === "chat" ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-white/10 hover:bg-white/20 text-white"
                    }`}
                >
                    <span className="material-symbols-outlined text-[22px]">chat_bubble</span>
                </button>

                <button
                    onClick={() => { setSidebarOpen(sidebarOpen && activeTab === "people" ? false : true); setActiveTab("people"); }}
                    className={`relative size-12 rounded-full flex items-center justify-center transition-all ${
                        sidebarOpen && activeTab === "people" ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-white/10 hover:bg-white/20 text-white"
                    }`}
                >
                    <span className="material-symbols-outlined text-[22px]">group</span>
                    {isHost && waitingCount > 0 && (
                        <span className="absolute top-0 right-0 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] text-white font-bold border border-background animate-bounce">
              {waitingCount}
            </span>
                    )}
                </button>

                <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
                    <span className="material-symbols-outlined text-[22px]">more_vert</span>
                </button>

                <div className="w-px h-8 bg-white/10 mx-1"></div>

                <button
                    onClick={handleDisconnect}
                    className="px-8 py-3 rounded-full bg-red-500 hover:bg-red-600 text-white font-black text-sm shadow-xl shadow-red-500/20 active:scale-95 transition-all"
                >
                    Leave Call
                </button>
            </div>
        </footer>
    );
};

export default ControlBar;