// UI-PtitMeet/pages/MeetingPage.tsx
import React, { useState, useEffect, useRef } from 'react'; // Thêm useRef
import { Participant } from '../types';
import { useNavigate } from 'react-router-dom';

const INITIAL_PARTICIPANTS: Participant[] = [
  { id: '1', name: 'Sarah Jenkins', avatar: 'https://picsum.photos/200?random=11', isSpeaking: true, isHost: true },
  { id: '2', name: 'em Tai sieu cap co bap (You)', avatar: 'https://picsum.photos/200?random=12', isMe: true },
  { id: '3', name: 'Van Minh Tan', avatar: 'https://picsum.photos/200?random=13', isMuted: true },
  { id: '4', name: 'Ngo Tan Sang', avatar: 'https://picsum.photos/200?random=14', isVideoOff: true },
  { id: '5', name: 'Vu Tien Dat', avatar: 'https://picsum.photos/200?random=15' },
  { id: '6', name: 'Development Room', avatar: 'https://picsum.photos/200?random=16' },
];

const MeetingPage: React.FC = () => {
  const navigate = useNavigate();
  const chatEndRef = useRef<HTMLDivElement>(null); // Để tự động cuộn chat

  // States
  const [participants] = useState<Participant[]>(INITIAL_PARTICIPANTS);
  const [activeTab, setActiveTab] = useState<'chat' | 'people'>('chat'); // State chuyển tab
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
  const [isRecording, setIsRecording] = useState(true);
  const [timer, setTimer] = useState(0);
  const [newMessage, setNewMessage] = useState('');
  const [chatMessages, setChatMessages] = useState([
    { id: 3, user: 'Van Minh Tan', text: 'Vu Tien Dat gay', time: '10:02 AM', isMe: false },
    { id: 5, user: 'Vu Tien Dat', text: 'Van Minh Tan la wibu', time: '10:03 AM', isMe: false },
  ]);

  // Tự động cuộn xuống khi có tin nhắn mới
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chatMessages, sidebarOpen]);

  useEffect(() => {
    const interval = setInterval(() => setTimer(t => t + 1), 1000);
    return () => clearInterval(interval);
  }, []);

  const sendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    setChatMessages([...chatMessages, {
      id: Date.now(),
      user: 'em Tai sieu cap co bap (You)',
      text: newMessage,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isMe: true
    }]);
    setNewMessage('');
  };

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h > 0 ? h + ':' : ''}${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="h-screen flex flex-col bg-background overflow-hidden text-white font-sans">
      {/* Header - Giữ nguyên logic cũ nhưng tinh chỉnh UI */}
      <header className="h-16 flex items-center justify-between px-6 bg-background/80 backdrop-blur-md z-30 shrink-0 border-b border-white/5">
        <div className="flex items-center gap-4">
          <div className="flex flex-col">
            <h1 className="text-sm font-bold leading-none mb-1">Weekly Product Sync</h1>
            <div className="flex items-center gap-2 text-[10px] text-gray-500 font-bold uppercase tracking-widest">
              <span>MTG-849-201</span>
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
            <span className="material-symbols-outlined text-green-500 text-[18px]">lock</span>
            <span className="text-[10px] font-bold text-gray-400 uppercase">Encrypted</span>
          </div>
          <button className="p-2 rounded-full hover:bg-white/5 text-gray-400"><span className="material-symbols-outlined">info</span></button>
        </div>
      </header>

      <div className="flex-grow flex relative">
        {/* Main Grid Area */}
        <main className={`flex-grow p-4 transition-all duration-300 ${sidebarOpen ? 'lg:mr-80' : ''}`}>
          <div className="h-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 content-center max-w-7xl mx-auto">
            {participants.map((p) => (
              <div 
                key={p.id}
                className={`group relative aspect-video bg-surface rounded-2xl overflow-hidden border-2 transition-all ${p.isSpeaking ? 'border-primary shadow-lg shadow-primary/20' : 'border-transparent'}`}
              >
                {!p.isVideoOff ? (
                  <img src={p.avatar} alt={p.name} className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-800 to-gray-900">
                    <div className="size-20 rounded-full bg-surface border border-white/10 flex items-center justify-center text-white text-2xl font-black shadow-2xl">
                      {p.name.charAt(0)}
                    </div>
                  </div>
                )}
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60"></div>
                <div className="absolute bottom-4 left-4 flex items-center gap-2">
                  <span className="text-xs font-bold text-white tracking-wide truncate max-w-[120px]">{p.name}</span>
                  {p.isMuted && <span className="material-symbols-outlined text-red-500 text-[16px]">mic_off</span>}
                  {p.isMe && <span className="bg-primary px-1.5 py-0.5 rounded text-[8px] font-bold uppercase tracking-wider">Me</span>}
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

        {/* Sidebar Hoàn thiện */}
        <aside className={`fixed top-16 right-0 bottom-24 w-80 bg-surface border-l border-white/5 z-20 transition-transform duration-300 shadow-2xl ${sidebarOpen ? 'translate-x-0' : 'translate-x-full'}`}>
          <div className="flex flex-col h-full">
            {/* Tabs Navigation */}
            <div className="flex p-2 gap-1 border-b border-white/5 bg-background/20">
              <button 
                onClick={() => setActiveTab('chat')}
                className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${activeTab === 'chat' ? 'bg-primary text-white shadow-lg' : 'text-gray-500 hover:bg-white/5'}`}
              >
                In-call Messages
              </button>
              <button 
                onClick={() => setActiveTab('people')}
                className={`flex-1 py-2 rounded-lg text-xs font-bold transition-all ${activeTab === 'people' ? 'bg-primary text-white shadow-lg' : 'text-gray-500 hover:bg-white/5'}`}
              >
                People ({participants.length})
              </button>
            </div>

            {/* Chat Content */}
            {activeTab === 'chat' && (
              <>
                <div className="flex-grow overflow-y-auto p-4 space-y-4 no-scrollbar">
                  <p className="text-[10px] text-center text-gray-500 font-bold uppercase tracking-widest bg-white/5 py-2 rounded-lg">Messages are visible only to people in the call</p>
                  {chatMessages.map(msg => (
                    <div key={msg.id} className={`flex flex-col ${msg.isMe ? 'items-end' : 'items-start'}`}>
                      <div className="flex items-center gap-2 mb-1 px-1">
                        {!msg.isMe && <span className="text-[10px] font-black text-gray-400">{msg.user}</span>}
                        <span className="text-[9px] text-gray-600">{msg.time}</span>
                      </div>
                      <div className={`max-w-[85%] p-3 text-sm leading-relaxed ${msg.isMe ? 'bg-primary text-white rounded-2xl rounded-tr-none shadow-md shadow-primary/10' : 'bg-white/5 text-gray-200 rounded-2xl rounded-tl-none border border-white/5'}`}>
                        {msg.text}
                      </div>
                    </div>
                  ))}
                  <div ref={chatEndRef} />
                </div>
                <form onSubmit={sendMessage} className="p-4 border-t border-white/5 bg-background/40">
                  <div className="relative">
                    <input 
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      placeholder="Send a message..."
                      className="w-full bg-surface border border-white/10 rounded-xl py-3 pl-4 pr-12 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 transition-all"
                    />
                    <button type="submit" className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 text-primary hover:bg-primary/10 rounded-lg transition-colors">
                      <span className="material-symbols-outlined">send</span>
                    </button>
                  </div>
                </form>
              </>
            )}

            {/* People Content */}
            {activeTab === 'people' && (
              <div className="flex-grow flex flex-col">
                <div className="p-4">
                  <button className="w-full py-2.5 rounded-xl border border-primary/30 text-primary text-xs font-bold hover:bg-primary/5 transition-colors flex items-center justify-center gap-2">
                    <span className="material-symbols-outlined text-sm">person_add</span>
                    Invite someone
                  </button>
                </div>
                <div className="flex-grow overflow-y-auto p-2 space-y-1 no-scrollbar">
                  {participants.map(p => (
                    <div key={p.id} className="flex items-center justify-between p-2 rounded-xl hover:bg-white/5 transition-colors group">
                      <div className="flex items-center gap-3">
                        <div className="relative">
                          <img src={p.avatar} className="size-8 rounded-full border border-white/10" alt="" />
                          {p.isSpeaking && <div className="absolute -bottom-0.5 -right-0.5 size-2.5 bg-green-500 rounded-full border-2 border-surface"></div>}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-semibold truncate max-w-[120px]">{p.name}</span>
                          {p.isHost && <span className="text-[9px] text-primary font-bold uppercase">Meeting Host</span>}
                        </div>
                      </div>
                      <div className="flex items-center gap-1 opacity-60 group-hover:opacity-100 transition-opacity">
                        <span className={`material-symbols-outlined text-[18px] ${p.isMuted ? 'text-red-500' : 'text-gray-400'}`}>
                          {p.isMuted ? 'mic_off' : 'mic'}
                        </span>
                        <button className="material-symbols-outlined text-[18px] text-gray-400 hover:text-white">more_vert</button>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="p-4 border-t border-white/5">
                  <button className="w-full py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-bold transition-colors">Mute All Participants</button>
                </div>
              </div>
            )}
          </div>
        </aside>
      </div>

      {/* Control Bar - Tinh chỉnh hiệu ứng shadow và nút Sidebar */}
      <footer className="h-24 flex items-center justify-center px-6 relative z-40 bg-background shrink-0 border-t border-white/5">
        <div className="flex items-center gap-3 bg-surface/90 backdrop-blur-xl p-2 rounded-full border border-white/10 shadow-2xl">
          <button 
            onClick={() => setIsMuted(!isMuted)}
            className={`size-12 rounded-full flex items-center justify-center transition-all ${!isMuted ? 'bg-white/10 hover:bg-white/20 text-white' : 'bg-red-500 text-white shadow-lg shadow-red-500/20'}`}
          >
            <span className="material-symbols-outlined">{!isMuted ? 'mic' : 'mic_off'}</span>
          </button>
          <button 
            onClick={() => setIsVideoOff(!isVideoOff)}
            className={`size-12 rounded-full flex items-center justify-center transition-all ${!isVideoOff ? 'bg-white/10 hover:bg-white/20 text-white' : 'bg-red-500 text-white shadow-lg shadow-red-500/20'}`}
          >
            <span className="material-symbols-outlined">{!isVideoOff ? 'videocam' : 'videocam_off'}</span>
          </button>
          <div className="w-px h-8 bg-white/10 mx-1"></div>
          <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">present_to_all</span>
          </button>
          <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">sentiment_satisfied</span>
          </button>
          <button 
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className={`size-12 rounded-full flex items-center justify-center transition-all ${sidebarOpen ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'bg-white/10 hover:bg-white/20 text-white'}`}
          >
            <span className="material-symbols-outlined text-[22px]">chat_bubble</span>
          </button>
          <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-all">
            <span className="material-symbols-outlined text-[22px]">more_vert</span>
          </button>
          <div className="w-px h-8 bg-white/10 mx-1"></div>
          <button 
            onClick={() => navigate('/summary')}
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