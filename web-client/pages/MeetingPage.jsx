import React, { useState, useEffect } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { meetingService } from "../services/meetingService";
import { Client } from "@stomp/stompjs";
import { useRef } from "react";

import ChatPanel from "../components/ChatPanel";
import { RiRecordCircleFill } from "react-icons/ri";

import { LiveKitRoom, RoomAudioRenderer } from "@livekit/components-react";
import "@livekit/components-styles";

import MeetingHeader from "../components/meeting-page/MeetingHeader.jsx";
import ParticipantGrid from "../components/meeting-page/ParticipantGrid.jsx";
import MeetingSidebar from "../components/meeting-page/MeetingSidebar";
import ControlBar from "../components/meeting-page/ControlBar";

const MeetingPage = () => {
  const { code } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();

  const joinData = location.state || {};
  const isHost = joinData.role === "HOST";
  const token = joinData.token;
  const serverUrl = joinData.serverUrl;

  const initialAudioEnabled = joinData.micOn ?? true;
  const initialVideoEnabled = joinData.camOn ?? true;

  const [activeTab, setActiveTab] = useState("chat");
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const [isRecord, setIsRecord] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
  const [isRecording, setIsRecording] = useState(true);

  const [timer, setTimer] = useState(0);
  const [egressId, setEgressId ] = useState("")

  const [toastMessage, setToastMessage] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const toastTimerRef = useRef(null);
  const isChatOpenRef = useRef(false);



  const [waitingList, setWaitingList] = useState([]);
  const [isLoadingWaiting, setIsLoadingWaiting] = useState(false);

  // MỚI: Thêm state để quản lý STOMP Client xài chung
  const [stompClient, setStompClient] = useState(null);
  const [isStompConnected, setIsStompConnected] = useState(false);

  useEffect(() => {
    if (!token || !serverUrl) {
      alert("Không tìm thấy thông tin phòng. Vui lòng tham gia lại!");
      navigate("/");
    }
  }, [token, serverUrl, navigate]);

  // CHỈNH SỬA: Quản lý STOMP Connection tập trung
  useEffect(() => {
    if (!code) return;

    if (isHost) fetchWaitingList();

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 5000,
      onConnect: () => {
        setIsStompConnected(true);

        // Nếu là Host thì đăng ký thêm kênh nhận thông báo phòng chờ
        if (isHost) {
          client.subscribe(`/topic/meeting/${code}/admin`, () => {
            fetchWaitingList();
          });
        }
      },
      onDisconnect: () => setIsStompConnected(false)
    });

    client.activate();
    setStompClient(client);

    return () => {
      if (client.active) client.deactivate();
    };
  }, [code, isHost]);

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
      setWaitingList((prev) => prev.filter((p) => p.participantId !== participantId));
    } catch (error) {
      alert("Lỗi xử lý duyệt: " + (error.response?.data?.message || ""));
    }
  };

  if (!token || !serverUrl) return <div className="h-screen bg-background flex items-center justify-center text-white">Đang kết nối...</div>;

  return (
      <LiveKitRoom video={initialVideoEnabled} audio={initialAudioEnabled} token={token} serverUrl={serverUrl} connect={true}>
        <div className="h-screen w-full flex flex-col bg-background overflow-hidden text-white font-sans">
          <MeetingHeader code={code} />
          <div className="flex-grow flex relative overflow-hidden w-full">
            <ParticipantGrid sidebarOpen={sidebarOpen} />
            <MeetingSidebar
                sidebarOpen={sidebarOpen}
                activeTab={activeTab}
                setActiveTab={setActiveTab}
                isHost={isHost}
                waitingList={waitingList}
                isLoadingWaiting={isLoadingWaiting}
                handleApproval={handleApproval}
                fetchWaitingList={fetchWaitingList}

                // MỚI: Truyền dữ liệu STOMP và user xuống Sidebar để Chat dùng
                stompClient={stompClient}
                isStompConnected={isStompConnected}
                currentUser={user}
                meetingCode={code}
            />
          </div>
          <ControlBar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} activeTab={activeTab} setActiveTab={setActiveTab} waitingCount={waitingList.length} isHost={isHost} />
          <RoomAudioRenderer />
        </div>
      </LiveKitRoom>
  );
};

export default MeetingPage;