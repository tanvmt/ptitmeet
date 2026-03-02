import React, { useState, useRef, useEffect } from "react";
import { useParticipants } from "@livekit/components-react";
import { meetingService } from "../../services/meetingService.js"; // Đảm bảo import đúng đường dẫn

const MeetingSidebar = ({
                            sidebarOpen, activeTab, setActiveTab,
                            isHost, waitingList, isLoadingWaiting, handleApproval, fetchWaitingList,
                            stompClient, isStompConnected, currentUser, meetingCode // Nhận props từ MeetingPage
                        }) => {
    const chatEndRef = useRef(null);
    const participants = useParticipants();

    // --- LOGIC CHAT TỪ CODE CỦA BẠN BẠN ---
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState("");
    const [isLoadingChat, setIsLoadingChat] = useState(false);

    // Lấy lịch sử chat khi mới vào
    useEffect(() => {
        if (meetingCode) {
            fetchHistory();
        }
    }, [meetingCode]);

    const fetchHistory = async () => {
        try {
            setIsLoadingChat(true);
            const data = await meetingService.getChatHistory(meetingCode);
            setMessages(data || []);
        } catch (error) {
            console.error("Lỗi tải lịch sử chat:", error);
        } finally {
            setIsLoadingChat(false);
        }
    };

    // Lắng nghe tin nhắn mới qua WebSocket
    useEffect(() => {
        if (!stompClient || !isStompConnected || !meetingCode) return;

        const chatSubscription = stompClient.subscribe(
            `/topic/meeting/${meetingCode}/chat`,
            (message) => {
                const newMsg = JSON.parse(message.body);
                setMessages((prev) => [...prev, newMsg]);
            }
        );

        return () => chatSubscription.unsubscribe();
    }, [stompClient, isStompConnected, meetingCode]);

    // Tự động cuộn xuống cuối
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages, sidebarOpen, activeTab]);

    // Gửi tin nhắn
    const handleSendMessage = (e) => {
        e.preventDefault();
        if (!inputMessage.trim() || !stompClient || !stompClient.active) return;

        const chatMessage = {
            senderId: currentUser.id || currentUser.userId, // Chỉnh lại theo đúng field name của object user
            senderName: currentUser.fullName,
            content: inputMessage.trim()
        };

        stompClient.publish({
            destination: `/app/meeting/${meetingCode}/chat.sendMessage`,
            body: JSON.stringify(chatMessage)
        });

        setInputMessage("");
    };

    const formatTime = (isoString) => {
        if (!isoString) return "";
        return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };
    // --------------------------------------

    return (
        <aside className={`fixed top-16 right-0 bottom-24 w-80 bg-surface border-l border-white/5 z-20 transition-transform duration-300 shadow-2xl ${sidebarOpen ? "translate-x-0" : "translate-x-full"}`}>
            <div className="flex flex-col h-full">
                {/* Tabs */}
                <div className="flex p-2 gap-1 border-b border-white/5 bg-background/20">
                    <button onClick={() => setActiveTab("chat")} className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${activeTab === "chat" ? "bg-primary text-white" : "text-gray-500 hover:bg-white/5"}`}>
                        Messages
                    </button>
                    <button onClick={() => setActiveTab("people")} className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${activeTab === "people" ? "bg-primary text-white" : "text-gray-500 hover:bg-white/5"}`}>
                        People ({participants.length})
                    </button>
                </div>

                {/* TAB CHAT ĐÃ ĐƯỢC THAY BẰNG LOGIC BACKEND */}
                {activeTab === "chat" && (
                    <div className="flex flex-col h-full">
                        <div className="flex-grow overflow-y-auto p-4 space-y-4 no-scrollbar">
                            <p className="text-[10px] text-center text-gray-500 font-bold uppercase tracking-widest bg-white/5 py-2 rounded-lg">
                                Messages are saved to history
                            </p>

                            {isLoadingChat ? (
                                <div className="text-center text-sm text-gray-500 mt-4">Loading messages...</div>
                            ) : (
                                messages.map((msg, idx) => {
                                    const isMe = msg.senderId === (currentUser.id || currentUser.userId);
                                    return (
                                        <div key={msg.id || idx} className={`flex flex-col ${isMe ? "items-end" : "items-start"}`}>
                                            <div className="flex items-center gap-2 mb-1 px-1">
                                                {!isMe && <span className="text-[10px] font-black text-gray-400">{msg.senderName}</span>}
                                                <span className="text-[9px] text-gray-600">{formatTime(msg.timestamp)}</span>
                                            </div>
                                            <div className={`max-w-[85%] p-3 text-sm leading-relaxed ${isMe ? "bg-primary text-white rounded-2xl rounded-tr-none shadow-md shadow-primary/10" : "bg-white/5 text-gray-200 rounded-2xl rounded-tl-none border border-white/5"}`}>
                                                {msg.content}
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                            <div ref={chatEndRef} />
                        </div>

                        <form onSubmit={handleSendMessage} className="p-4 border-t border-white/5 bg-background/40">
                            <div className="relative">
                                <input
                                    value={inputMessage}
                                    onChange={(e) => setInputMessage(e.target.value)}
                                    placeholder="Send a message..."
                                    className="w-full bg-surface border border-white/10 rounded-xl py-3 pl-4 pr-12 text-sm text-white focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    disabled={!isStompConnected}
                                />
                                <button
                                    type="submit"
                                    disabled={!inputMessage.trim() || !isStompConnected}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 text-primary hover:bg-primary/10 disabled:text-gray-600 rounded-lg transition-colors"
                                >
                                    <span className="material-symbols-outlined">send</span>
                                </button>
                            </div>
                        </form>
                    </div>
                )}

                {/* TAB PEOPLE & PHÒNG CHỜ (Giữ nguyên như cũ) */}
                {activeTab === "people" && (
                    // ... (Phần code hiển thị Waiting Room và Danh sách người trong phòng giữ nguyên như file trước)
                    <div className="flex-grow flex flex-col overflow-y-auto no-scrollbar">
                        {/* ... */}
                    </div>
                )}
            </div>
        </aside>
    );
};

export default MeetingSidebar;