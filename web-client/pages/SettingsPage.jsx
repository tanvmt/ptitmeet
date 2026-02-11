import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const SettingsPage = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('audio');

  // Mock states cho các tùy chọn
  const [noiseCancellation, setNoiseCancellation] = useState(true);
  const [echoReduction, setEchoReduction] = useState(false);
  const [autoVolume, setAutoVolume] = useState(true);

  const navItems = [
    { id: 'audio', label: 'Audio', icon: 'mic' },
    { id: 'video', label: 'Video', icon: 'videocam' },
    { id: 'background', label: 'Background', icon: 'wallpaper' },
    { id: 'notifications', label: 'Notifications', icon: 'notifications' },
    { id: 'profile', label: 'Profile', icon: 'person' },
    { id: 'security', label: 'Security', icon: 'security' },
  ];

  const handleSave = () => {
    alert('Settings saved successfully!');
    navigate('/dashboard');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 font-display">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={() => navigate('/dashboard')}
      ></div>

      {/* Settings Modal */}
      <div className="relative w-full max-w-2xl h-[640px] bg-surface-dark rounded-2xl shadow-2xl border border-white/10 flex overflow-hidden flex-col md:flex-row animate-in fade-in zoom-in-95 duration-200 text-white">

        {/* Sidebar */}
        <aside className="w-full md:w-52 bg-surface-darker flex flex-col border-b md:border-b-0 md:border-r border-white/5 shrink-0">
          <div className="p-5">
            <h2 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
              <span className="material-symbols-outlined text-primary">settings</span>
              Settings
            </h2>
          </div>

          <nav className="flex-1 px-3 py-2 flex flex-col gap-1 overflow-y-auto no-scrollbar">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all group w-full text-left ${activeTab === item.id
                  ? 'bg-primary text-white shadow-md shadow-primary/10'
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
                  }`}
              >
                <span className={`material-symbols-outlined text-[20px] ${activeTab === item.id ? 'text-white' : 'group-hover:text-white'}`}>
                  {item.icon}
                </span>
                <span className="text-sm font-medium">{item.label}</span>
              </button>
            ))}
          </nav>

          <div className="p-4 border-t border-white/5 mt-auto">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-primary to-purple-500 flex items-center justify-center text-xs font-bold ring-2 ring-surface-dark">
                JD
              </div>
              <div className="flex flex-col overflow-hidden">
                <span className="text-xs font-semibold text-white truncate">em Tai sieu cap co bap</span>
                <span className="text-[10px] text-gray-500 truncate">Pro Member</span>
              </div>
            </div>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 flex flex-col h-full bg-surface-dark relative min-w-0">
          {/* Content Header */}
          <div className="flex items-start justify-between px-6 pt-6 pb-2 shrink-0">
            <div>
              <h1 className="text-xl font-bold text-white capitalize">{activeTab}</h1>
              <p className="text-gray-400 text-xs mt-1">
                {activeTab === 'audio' && 'Manage input and output devices.'}
                {activeTab === 'video' && 'Configure your camera and quality settings.'}
                {activeTab === 'profile' && 'Update your personal information.'}
                {!['audio', 'video', 'profile'].includes(activeTab) && `Adjust your ${activeTab} preferences.`}
              </p>
            </div>
            <button
              onClick={() => navigate('/dashboard')}
              className="p-1.5 -mr-2 rounded-full hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          {/* Tab Content */}
          <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6 no-scrollbar pb-24">
            {activeTab === 'audio' && (
              <>
                {/* Microphone Section */}
                <div className="space-y-3">
                  <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider">Microphone</label>
                  <div className="relative group">
                    <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-[18px]">mic</span>
                    <select className="w-full h-10 pl-10 pr-8 bg-input-bg text-white border border-white/5 rounded-lg focus:ring-1 focus:ring-primary outline-none text-sm appearance-none">
                      <option>Default - MacBook Pro Mic</option>
                      <option>Blue Yeti X</option>
                      <option>AirPods Pro</option>
                    </select>
                  </div>

                  {/* Input Level Mock */}
                  <div className="bg-surface-darker/50 rounded-lg p-3 border border-white/5 flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-surface-dark flex items-center justify-center text-primary shrink-0">
                      <span className="material-symbols-outlined text-[18px]">graphic_eq</span>
                    </div>
                    <div className="flex-1 flex flex-col gap-1">
                      <div className="flex justify-between text-[10px] text-gray-400 font-medium uppercase">
                        <span>Input Level</span>
                        <span className="text-primary">Good</span>
                      </div>
                      <div className="h-1.5 w-full bg-surface-dark rounded-full overflow-hidden">
                        <div className="h-full bg-primary rounded-full transition-all duration-300" style={{ width: '75%' }}></div>
                      </div>
                    </div>
                  </div>

                  <label className="flex items-center gap-2 cursor-pointer group">
                    <input
                      type="checkbox"
                      checked={autoVolume}
                      onChange={() => setAutoVolume(!autoVolume)}
                      className="rounded border-gray-600 bg-surface-darker text-primary focus:ring-0 focus:ring-offset-0 size-4"
                    />
                    <span className="text-sm text-gray-400 group-hover:text-gray-300 transition-colors">Auto-adjust volume</span>
                  </label>
                </div>

                <div className="h-px bg-white/5 w-full"></div>

                {/* Speakers Section */}
                <div className="space-y-3">
                  <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider">Speakers</label>
                  <div className="flex gap-3 items-start flex-col sm:flex-row">
                    <div className="relative flex-1 w-full">
                      <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-[18px]">volume_up</span>
                      <select className="w-full h-10 pl-10 pr-8 bg-input-bg text-white border border-white/5 rounded-lg outline-none text-sm appearance-none">
                        <option>Default - MacBook Pro Speakers</option>
                        <option>External Headphones</option>
                      </select>
                    </div>
                    <button className="h-10 px-4 bg-white/5 hover:bg-white/10 rounded-lg text-sm font-medium border border-white/5 transition-colors flex items-center gap-2">
                      <span className="material-symbols-outlined text-[16px]">play_arrow</span>
                      Test
                    </button>
                  </div>
                </div>

                {/* Enhancements */}
                <div className="space-y-3">
                  <h3 className="text-xs font-semibold text-gray-300 uppercase tracking-wider">Enhancements</h3>
                  <div className="flex items-center justify-between p-3 rounded-xl bg-input-bg border border-transparent hover:border-white/5 transition-colors group">
                    <div className="flex gap-3 items-center">
                      <div className="p-1.5 rounded-lg bg-surface-dark text-gray-400 group-hover:text-primary transition-colors">
                        <span className="material-symbols-outlined text-[18px]">blur_on</span>
                      </div>
                      <div>
                        <p className="text-sm font-medium text-white">Noise Cancellation</p>
                        <p className="text-[11px] text-gray-400">Reduce background noise</p>
                      </div>
                    </div>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input
                        type="checkbox"
                        checked={noiseCancellation}
                        onChange={() => setNoiseCancellation(!noiseCancellation)}
                        className="sr-only peer"
                      />
                      <div className="w-9 h-5 bg-gray-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary"></div>
                    </label>
                  </div>
                  {/* Echo Reduction tương tự... */}
                </div>
              </>
            )}

            {activeTab !== 'audio' && (
              <div className="flex flex-col items-center justify-center h-full opacity-30 py-20">
                <span className="material-symbols-outlined text-6xl mb-4">construction</span>
                <p className="text-sm font-medium">The {activeTab} settings are under development.</p>
              </div>
            )}
          </div>

          {/* Footer Actions */}
          <div className="absolute bottom-0 left-0 right-0 p-4 bg-surface-dark/95 backdrop-blur-md border-t border-white/5 flex items-center justify-end gap-3 z-10">
            <button
              onClick={() => navigate('/dashboard')}
              className="px-5 h-9 rounded-lg text-sm font-medium text-gray-300 hover:text-white hover:bg-white/5 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-6 h-9 rounded-lg bg-primary hover:bg-blue-600 text-sm font-medium text-white shadow-lg shadow-blue-500/20 transition-all active:scale-95"
            >
              Save Changes
            </button>
          </div>
        </main>
      </div>
    </div>
  );
};

export default SettingsPage;