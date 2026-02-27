
import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';


const SummaryPage = () => {
  const navigate = useNavigate();

  //Mock Random Meeting Data
  const stats = useMemo(() => ({
    participants: Math.floor(Math.random() * 15) + 5,
    messages: Math.floor(Math.random() * 100) + 20,
    duration: "42m 15s",
    quality: 4.8
  }), []);

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="max-w-2xl w-full text-center">
        <div className="size-20 bg-slate-200 dark:bg-surface rounded-full flex items-center justify-center text-slate-500 dark:text-slate-400 mx-auto mb-8 shadow-inner">
          <span className="material-symbols-outlined text-4xl">waving_hand</span>
        </div>
        <h1 className="text-4xl font-black mb-4">You left the meeting</h1>
        <p className="text-gray-400 mb-12">Weekly Product Sync • {stats.duration} duration</p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center mb-16">
          <button
            onClick={() => navigate('/waiting-room')}
            className="px-8 h-14 bg-primary hover:bg-blue-600 text-white font-black rounded-full shadow-xl shadow-primary/20 transition-all active:scale-95 flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined">replay</span>
            Rejoin Meeting
          </button>
          <button
            onClick={() => navigate('/')}
            className="px-8 h-14 bg-white/5 hover:bg-white/10 text-white font-bold rounded-full border border-white/5 transition-all flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined">dashboard</span>
            Return Home
          </button>
        </div>

        <div className="bg-surface rounded-2xl border border-white/5 overflow-hidden text-left shadow-2xl">
          <div className="p-6 border-b border-white/5 flex items-center justify-between">
            <h2 className="text-sm font-bold text-gray-500 uppercase tracking-widest">Meeting Summary</h2>
            <span className="text-xs font-bold text-green-500 bg-green-500/10 px-2 py-1 rounded">Archived</span>
          </div>
          <div className="grid grid-cols-2 divide-x divide-white/5">
            <div className="p-6 space-y-6">
              <div className="flex items-center gap-4">
                <div className="size-10 bg-primary/10 rounded-xl flex items-center justify-center text-primary"><span className="material-symbols-outlined">group</span></div>
                <div>
                  <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">Participants</p>
                  <p className="text-lg font-black">{stats.participants} Active</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="size-10 bg-orange-500/10 rounded-xl flex items-center justify-center text-orange-500"><span className="material-symbols-outlined">chat</span></div>
                <div>
                  <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">Chat Messages</p>
                  <p className="text-lg font-black">{stats.messages} Sent</p>
                </div>
              </div>
            </div>
            <div className="p-6 space-y-6">
              <div className="flex items-center gap-4">
                <div className="size-10 bg-blue-500/10 rounded-xl flex items-center justify-center text-blue-500"><span className="material-symbols-outlined">radio_button_checked</span></div>
                <div>
                  <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">Recording</p>
                  <p className="text-sm font-bold text-blue-400">Processing...</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="size-10 bg-purple-500/10 rounded-xl flex items-center justify-center text-purple-500"><span className="material-symbols-outlined">star</span></div>
                <div>
                  <p className="text-xs font-bold text-gray-500 uppercase tracking-tighter">Quality Rating</p>
                  <div className="flex text-yellow-400">
                    <span className="material-symbols-filled text-[18px]">star</span>
                    <span className="material-symbols-filled text-[18px]">star</span>
                    <span className="material-symbols-filled text-[18px]">star</span>
                    <span className="material-symbols-filled text-[18px]">star</span>
                    <span className="material-symbols-outlined text-[18px]">star</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SummaryPage;
