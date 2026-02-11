
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const WaitingRoomPage = ({ userName }) => {
  const navigate = useNavigate();
  const [micOn, setMicOn] = useState(true);
  const [videoOn, setVideoOn] = useState(true);
  const [isCheckingDevices, setIsCheckingDevices] = useState(true);

  //Mock Check Devices Call
  useEffect(() => {
    const timer = setTimeout(() => setIsCheckingDevices(false), 1500);
    return () => clearTimeout(timer);
  }, []);

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
          <button className="p-2 rounded-full hover:bg-white/5 text-gray-400"><span className="material-symbols-outlined">help</span></button>
          <button className="p-2 rounded-full hover:bg-white/5 text-gray-400"><span className="material-symbols-outlined">settings</span></button>
        </div>
      </header>

      <main className="flex-grow flex items-center justify-center p-6">
        <div className="max-w-5xl w-full grid lg:grid-cols-2 gap-12 items-center">
          <div className="space-y-8">
            <div className="relative aspect-video bg-surface rounded-2xl overflow-hidden shadow-2xl border border-white/10 group">

              {isCheckingDevices ? (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/40 backdrop-blur-sm z-20">
                  <div className="size-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin mb-4"></div>
                  <p className="text-sm font-bold text-gray-300">Checking camera & mic...</p>
                </div>
              ) : (
                <img
                  src="https://picsum.photos/1280/720?random=2"
                  alt="Camera Preview"
                  className={`w-full h-full object-cover transition-opacity duration-500 ${videoOn ? 'opacity-100' : 'opacity-0'}`}
                />)}

              {!videoOn && (
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="size-32 rounded-full bg-primary/20 flex items-center justify-center text-primary text-5xl font-black">
                    {userName.charAt(0)}
                  </div>
                </div>
              )}
              <div className="absolute top-4 right-4 flex items-center gap-2 bg-black/40 backdrop-blur px-3 py-1 rounded-full border border-white/10">
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></div>
                <span className="text-[10px] font-bold uppercase tracking-wider">HD Live</span>
              </div>
              <div className="absolute bottom-6 left-0 right-0 flex justify-center gap-4 z-10">
                <button
                  onClick={() => setMicOn(!micOn)}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${micOn ? 'bg-white/10 hover:bg-white/20 text-white' : 'bg-red-500 text-white'}`}
                >
                  <span className="material-symbols-outlined">{micOn ? 'mic' : 'mic_off'}</span>
                </button>
                <button
                  onClick={() => setVideoOn(!videoOn)}
                  className={`size-12 rounded-full flex items-center justify-center transition-all ${videoOn ? 'bg-primary hover:bg-blue-600 text-white' : 'bg-red-500 text-white'}`}
                >
                  <span className="material-symbols-outlined">{videoOn ? 'videocam' : 'videocam_off'}</span>
                </button>
                <button className="size-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center">
                  <span className="material-symbols-outlined">auto_awesome</span>
                </button>
              </div>
            </div>
          </div>

          <div className="space-y-8 text-center lg:text-left">
            <div>
              <h1 className="text-4xl font-black mb-4">Ready to join?</h1>
              <p className="text-gray-400">2 other people are already in this call.</p>
            </div>

            <div className="space-y-6">
              <div className="space-y-2">
                <label className="text-sm font-bold text-gray-500 block">Your Display Name</label>
                <input
                  type="text"
                  value={userName}
                  readOnly
                  className="w-full h-14 px-5 rounded-2xl bg-surface border border-white/10 text-lg font-bold"
                />
              </div>

              <div className="space-y-4">
                <button
                  onClick={() => navigate('/meeting')}
                  className="w-full h-16 bg-primary hover:bg-blue-600 text-white text-xl font-black rounded-2xl shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-[0.98]"
                >
                  Join Meeting
                </button>
                <button
                  onClick={() => navigate('/')}
                  className="w-full h-16 bg-transparent hover:bg-white/5 text-gray-400 font-bold rounded-2xl transition-all"
                >
                  Cancel
                </button>
              </div>
            </div>

            <div className="flex flex-col gap-3 p-4 bg-white/5 rounded-2xl border border-white/5 text-sm text-gray-500">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-primary">headphones</span>
                <span>Mic: {isCheckingDevices ? 'Detecting...' : 'MacBook Pro Microphone (System)'}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default WaitingRoomPage;
