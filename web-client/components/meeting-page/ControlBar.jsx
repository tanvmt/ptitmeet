import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useRoomContext, useLocalParticipant } from "@livekit/components-react";
import LeaveModal from "./LeaveModal";
import { meetingService } from "../../services/meetingService";

const ControlBar = ({
    sidebarOpen, setSidebarOpen, activeTab, setActiveTab, waitingCount, isHost, code, stompClient
}) => {
    const navigate = useNavigate();
    const room = useRoomContext(); // Lấy context phòng của LiveKit
    const [isRecord, setIsRecord] = useState(false)
    const [egressId, setEgressId] = useState(null)
    const egressIdRef = useRef(null);
    const { localParticipant, isMicrophoneEnabled, isCameraEnabled, isScreenShareEnabled } = useLocalParticipant();
    const [showLeaveModal, setShowLeaveModal] = useState(false);
    const [isHandRaised, setIsHandRaised] = useState(false); // State for raise hand
    const [showReactions, setShowReactions] = useState(false);

    const handleRecordMeeting = async () => {
        try {
            if (!isRecord) {
                const recordRes = await meetingService.startRecordMeeting(code);
                console.log("Start recording response:", JSON.stringify(recordRes.data));
                const id = recordRes.data?.data?.egressId || recordRes.data?.data?.egress_id;
                console.log("Extracted egressId:", id);
                setEgressId(id);
                egressIdRef.current = id;
            } else {
                const currentEgressId = egressIdRef.current;
                console.log("Stopping with egressId:", currentEgressId);
                await meetingService.endRecordMeeting(currentEgressId);
                setEgressId(null);
                egressIdRef.current = null;
            }
            setIsRecord(!isRecord);
        } catch (error) {
            console.error("Lỗi ghi hình:", error);
            alert("Lỗi ghi hình: " + (error.response?.data?.message || error.message));
        }
    };
    
    const toggleMic = async () => {
        if (localParticipant) {
            if (!isMicrophoneEnabled) {
                try {
                    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
                    stream.getTracks().forEach((track) => track.stop());
                } catch (error) {
                    console.error("Microphone permission denied:", error);
                    alert("Trình duyệt chưa cấp quyền microphone.");
                    return;
                }
            }
            await localParticipant.setMicrophoneEnabled(!isMicrophoneEnabled);
        }
    };

    const toggleCam = async () => {
        if (localParticipant) {
            if (!isCameraEnabled) {
                try {
                    const stream = await navigator.mediaDevices.getUserMedia({ video: true });
                    stream.getTracks().forEach((track) => track.stop());
                } catch (error) {
                    console.error("Camera permission denied:", error);
                    alert("Trình duyệt chưa cấp quyền camera.");
                    return;
                }
            }
            await localParticipant.setCameraEnabled(!isCameraEnabled);
        }
    };

    const toggleScreenShare = async () => {
        if (localParticipant) {
            await localParticipant.setScreenShareEnabled(!isScreenShareEnabled);
        }
    };

    const toggleHandRaise = async () => {
        const newHandRaisedState = !isHandRaised;
        setIsHandRaised(newHandRaisedState);
        
        // Phát event locally cho chính mình
        if (localParticipant) {
            const event = new CustomEvent('hand_raise', { 
                detail: { identity: localParticipant.identity, isRaised: newHandRaisedState }
            });
            window.dispatchEvent(event);
            
            // Gửi cho người khác trong phòng qua DataChannel
            if (room) {
                const encoder = new TextEncoder();
                const data = JSON.stringify({ isRaised: newHandRaisedState });
                await room.localParticipant.publishData(encoder.encode(data), {
                    reliable: true,
                    topic: 'hand_raise'
                });
            }
        }
    };

    const sendReaction = async (emoji) => {
        if (room && localParticipant) {
            const reactionPayload = {
                emoji,
                senderId: localParticipant.identity,
                senderName: localParticipant.name || localParticipant.identity || "Bạn",
            };

            window.dispatchEvent(new CustomEvent("reaction", {
                detail: reactionPayload,
            }));

            const encoder = new TextEncoder();
            const data = JSON.stringify(reactionPayload);
            await room.localParticipant.publishData(encoder.encode(data), {
                reliable: true,
                topic: 'reaction'
            });
        }
    };

    const handleProcessLeave = async (action) => {
        try {
            if (action === "END") {
                await meetingService.endMeetingForAll(code);
            } else {
                await meetingService.leaveMeeting(code);
            }

            if (stompClient && stompClient.active) {
                stompClient.deactivate();
            }

            if (room) {
                room.disconnect();
            }

            navigate("/summary", {
                state: {
                    meetingCode: code,
                    actionTaken: action,
                    isHost: isHost
                }
            });
        } catch (error) {
            alert("Lỗi khi thoát phòng: " + error.message);
        }
    };

    const onLeaveButtonClicked = () => {
        if (isHost) {
            setShowLeaveModal(true);
        } else {
            setShowLeaveModal(true);
        }
    };

    return (
        <>
            <LeaveModal
                isOpen={showLeaveModal}
                onClose={() => setShowLeaveModal(false)}
                onConfirm={handleProcessLeave}
                isHost={isHost}
            />
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

                    {/* NÚT GIƠ TAY */}
                    <button
                        onClick={toggleHandRaise}
                        className={`size-12 rounded-full flex items-center justify-center transition-all ${
                            isHandRaised ? "bg-primary text-white shadow-lg shadow-primary/20" : "bg-white/10 hover:bg-white/20 text-white"
                        }`}
                    >
                        <span className="material-symbols-outlined text-[22px]">front_hand</span>
                    </button>

                    {/* NÚT THẢ CẢM XÚC */}
                    <div className="relative">
                        <button 
                            onClick={() => setShowReactions(!showReactions)}
                            className={`size-12 rounded-full flex items-center justify-center transition-all ${
                                showReactions ? "bg-white/30 text-white" : "bg-white/10 hover:bg-white/20 text-white"
                            }`}
                        >
                            <span className="material-symbols-outlined text-[22px]">sentiment_satisfied</span>
                        </button>
                        
                        {showReactions && (
                            <div className="absolute bottom-16 left-1/2 -translate-x-1/2 flex gap-2 bg-surface/90 backdrop-blur-xl p-2 rounded-full border border-white/10 shadow-2xl">
                                {['👍', '❤️', '👏', '😂', '🎉', '😮'].map(emoji => (
                                    <button
                                        key={emoji}
                                        onClick={() => sendReaction(emoji)}
                                        className="text-2xl hover:scale-125 transition-transform p-2 rounded-full hover:bg-white/10"
                                    >
                                        {emoji}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

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
                        {/* nút ghi hình */}
                    <button
                        onClick={handleRecordMeeting}
                        className={`size-12 rounded-full flex items-center justify-center transition-all ${isRecord ? "bg-red-500 text-white shadow-lg shadow-red-500/20" : "bg-white/10 hover:bg-white/20 text-white"
                            }`}
                    >

                        <span className="material-symbols-outlined text-[22px]">{isRecord ? "screen_record" : "fiber_manual_record"}</span>
                    </button>

                    <button
                        onClick={onLeaveButtonClicked}
                        className="px-8 py-3 rounded-full bg-red-500 hover:bg-red-600 text-white font-black text-sm shadow-xl shadow-red-500/20 active:scale-95 transition-all"
                    >
                        Leave Call
                    </button>
                </div>
            </footer>
        </>
    );
};

export default ControlBar;
