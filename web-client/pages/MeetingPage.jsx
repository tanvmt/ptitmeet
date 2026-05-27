import React, { useState, useEffect } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { meetingService } from "../services/meetingService";
import { Client } from "@stomp/stompjs";

import { LiveKitRoom, RoomAudioRenderer } from "@livekit/components-react";
import "@livekit/components-styles";

import MeetingHeader from "../components/meeting-page/MeetingHeader.jsx";
import ParticipantGrid from "../components/meeting-page/ParticipantGrid.jsx";
import MeetingSidebar from "../components/meeting-page/MeetingSidebar";
import ControlBar from "../components/meeting-page/ControlBar";
import Reactions from "../components/meeting-page/Reactions"; // Import Reactions component
import {
  getWebSocketUrl,
  parseSystemAction,
  SYSTEM_ACTION_TYPES,
} from "../utils/meetingRealtime";

const MeetingPage = () => {
  const { code } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();

  const joinData = location.state || {};
  const [userRole, setUserRole] = useState(joinData.role || "ATTENDEE");
  const isHost = userRole === "HOST";
  const isCoHost = userRole === "CO_HOST";
  const token = joinData.token;
  const serverUrl = joinData.serverUrl;

  const initialAudioEnabled = joinData.micOn ?? true;
  const initialVideoEnabled = joinData.camOn ?? joinData.videoOn ?? true;
  const currentUserId = String(user?.userId || user?.id || "");

  const [activeTab, setActiveTab] = useState("chat");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [meetingSettings, setMeetingSettings] = useState(() => {
    try {
      if (joinData.settings) {
        return typeof joinData.settings === "string" ? JSON.parse(joinData.settings) : joinData.settings;
      }
    } catch (e) {
      console.error("Error parsing settings", e);
    }
    return {
      waitingRoom: true,
      muteAudioOnEntry: false,
      muteVideoOnEntry: false,
      chatEnabled: true,
      screenShareEnabled: true,
    };
  });

  const [waitingList, setWaitingList] = useState([]);
  const [isLoadingWaiting, setIsLoadingWaiting] = useState(false);
  const [unreadMessages, setUnreadMessages] = useState(0);
  const [chatToast, setChatToast] = useState(null);

  const [stompClient, setStompClient] = useState(null);
  const [isStompConnected, setIsStompConnected] = useState(false);

  useEffect(() => {
    if (!token || !serverUrl) {
      alert("Meeting details were not found. Please join again.");
      navigate("/");
    }
  }, [token, serverUrl, navigate]);

  useEffect(() => {
    if (sidebarOpen && activeTab === "chat") {
      setUnreadMessages(0);
      setChatToast(null);
    }
  }, [activeTab, sidebarOpen]);

  useEffect(() => {
    if (!chatToast) return;
    const timeoutId = window.setTimeout(() => setChatToast(null), 3500);
    return () => window.clearTimeout(timeoutId);
  }, [chatToast]);

  useEffect(() => {
    if (!code) return;

    if (isHost || isCoHost) fetchWaitingList();

    const client = new Client({
      brokerURL: getWebSocketUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        setIsStompConnected(true);

        if (isHost || isCoHost) {
          client.subscribe(`/topic/meeting/${code}/admin`, () => {
            fetchWaitingList();
          });
        }

        client.subscribe(`/topic/meeting/${code}/system`, (message) => {
            const action = parseSystemAction(message.body);
            if (!action?.type) return;

            const isTargetedAtCurrentUser =
              !action.targetParticipantId ||
              String(action.targetParticipantId) === currentUserId;

            if (action.type === "SETTINGS_UPDATED") {
              setMeetingSettings(action.settings);
            } else if (action.type === "ROLE_CHANGED") {
              if (String(action.targetUserId) === currentUserId) {
                setUserRole(action.role);
              }
              window.dispatchEvent(new CustomEvent('PARTICIPANTS_CHANGED'));
            } else if (action.type === "HOST_CHANGED") {
              if (String(action.newHostId) === currentUserId) {
                setUserRole("HOST");
              } else if (isHost) {
                setUserRole("ATTENDEE");
              }
              window.dispatchEvent(new CustomEvent('PARTICIPANTS_CHANGED'));
            } else if (action.type === SYSTEM_ACTION_TYPES.MEETING_ENDED) {
               if (!isHost) {
                   navigate("/summary", { 
                       state: { meetingCode: code, actionTaken: "ENDED_BY_HOST" } 
                   });
               }
            } else if (
              !(isHost || isCoHost) &&
              (action.type === SYSTEM_ACTION_TYPES.MUTE_ALL ||
                (action.type === SYSTEM_ACTION_TYPES.MUTE_PARTICIPANT && isTargetedAtCurrentUser))
            ) {
               window.dispatchEvent(new CustomEvent('SYSTEM_ACTION', { detail: action.type }));
            } else if (
              !(isHost || isCoHost) &&
              (action.type === SYSTEM_ACTION_TYPES.STOP_CAMERA_ALL ||
                (action.type === SYSTEM_ACTION_TYPES.STOP_CAMERA_PARTICIPANT && isTargetedAtCurrentUser))
            ) {
               window.dispatchEvent(new CustomEvent('SYSTEM_ACTION', { detail: action.type }));
             } else if (
              !(isHost || isCoHost) &&
              (action.type === SYSTEM_ACTION_TYPES.KICK_ALL ||
                (action.type === SYSTEM_ACTION_TYPES.KICK_PARTICIPANT && isTargetedAtCurrentUser))
            ) {
                navigate("/summary", { 
                    state: { meetingCode: code, actionTaken: "KICKED" } 
                });
             }
        });
      },
      onDisconnect: () => setIsStompConnected(false),
      onStompError: (frame) => {
        console.error("STOMP error:", frame.headers["message"], frame.body);
        setIsStompConnected(false);
      },
      onWebSocketError: (error) => {
        console.error("WebSocket error:", error);
        setIsStompConnected(false);
      }
    });

    client.activate();
    setStompClient(client);

    return () => {
      if (client.active) client.deactivate();
    };
  }, [code, currentUserId, isHost, isCoHost, navigate]);

  const fetchWaitingList = async () => {
    try {
      setIsLoadingWaiting(true);
      const data = await meetingService.getWaitingList(code);
      setWaitingList(data);
    } catch (error) {
      console.error("Unable to load waiting list:", error);
    } finally {
      setIsLoadingWaiting(false);
    }
  };

  const handleApproval = async (participantId, action) => {
    try {
      await meetingService.processApproval(code, participantId, action);
      setWaitingList((prev) => prev.filter((p) => p.participantId !== participantId));
    } catch (error) {
      alert("Unable to process the request: " + (error.response?.data?.message || ""));
    }
  };

  const handleIncomingMessage = (message) => {
    const senderId = String(message?.senderId || "");
    if (!message || senderId === currentUserId) return;

    const isChatOpen = sidebarOpen && activeTab === "chat";
    if (!isChatOpen) {
      setUnreadMessages((prev) => prev + 1);
      setChatToast({
        senderName: message.senderName || "New message",
        content: message.content || "",
      });
    }

    if (document.hidden && "Notification" in window && Notification.permission === "granted") {
      new Notification(message.senderName || "New message", {
        body: message.content || "",
      });
    }
  };

  if (!token || !serverUrl) return <div className="h-screen bg-background flex items-center justify-center text-white">Connecting...</div>;

  return (
      <LiveKitRoom video={initialVideoEnabled} audio={initialAudioEnabled} token={token} serverUrl={serverUrl} connect={true}>
        <div className="h-screen w-full flex flex-col bg-background overflow-hidden text-white font-sans">
          {chatToast && (
            <div className="pointer-events-none fixed top-20 right-6 z-[120] w-80 rounded-2xl border border-white/10 bg-surface/95 p-4 shadow-2xl backdrop-blur">
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-primary">New message</p>
              <p className="mt-2 text-sm font-semibold text-white">{chatToast.senderName}</p>
              <p className="mt-1 line-clamp-2 text-sm text-gray-300">{chatToast.content}</p>
            </div>
          )}
          <MeetingHeader code={code} />
          <div className="flex-grow flex relative overflow-hidden w-full">
            <ParticipantGrid sidebarOpen={sidebarOpen} />
            <MeetingSidebar
                sidebarOpen={sidebarOpen}
                activeTab={activeTab}
                setActiveTab={setActiveTab}
                isHost={isHost}
                isCoHost={isCoHost}
                userRole={userRole}
                waitingList={waitingList}
                isLoadingWaiting={isLoadingWaiting}
                handleApproval={handleApproval}
                fetchWaitingList={fetchWaitingList}
                stompClient={stompClient}
                isStompConnected={isStompConnected}
                currentUser={user}
                meetingCode={code}
                onIncomingMessage={handleIncomingMessage}
                meetingSettings={meetingSettings}
            />
          </div>
          <ControlBar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} activeTab={activeTab} setActiveTab={setActiveTab} waitingCount={waitingList.length} unreadCount={unreadMessages} isHost={isHost} isCoHost={isCoHost} code={code} stompClient={stompClient} meetingSettings={meetingSettings}/>
          <RoomAudioRenderer />
          <Reactions /> {/* Add Reactions component here */}
        </div>
      </LiveKitRoom>
  );
};

export default MeetingPage;
