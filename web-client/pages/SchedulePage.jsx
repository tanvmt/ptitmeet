import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // Thêm Link để điều hướng tối ưu

const SchedulePage = () => {
  const navigate = useNavigate();

  // Mock State cho Form
  const [formData, setFormData] = useState({
    title: '',
    date: 'October 5, 2023',
    time: '10:00 AM',
    duration: '15m',
    participants: ['sarah.j@meetpro.com', 'dev.team@meetpro.com']
  });

  const handleCreate = (e) => {
    e.preventDefault();
    console.log("Meeting Scheduled:", formData);
    alert(`Đã tạo cuộc họp: ${formData.title}`);
    navigate('/');
  };

  return (
    <div className="h-screen flex flex-col bg-background-light dark:bg-background-dark text-slate-900 dark:text-white font-display">

      {/* 1. Top Navigation (NavBar) - Bổ sung thanh bar phía trên */}
      <header className="flex items-center justify-between whitespace-nowrap border-b border-solid border-slate-200 dark:border-[#283039] bg-white dark:bg-[#111418] px-6 py-3 shrink-0 z-50">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-3 cursor-pointer" onClick={() => navigate('/')}>
            <div className="size-8 text-primary">
              <svg fill="currentColor" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg">
                <path d="M13.8261 17.4264C16.7203 18.1174 20.2244 18.5217 24 18.5217C27.7756 18.5217 31.2797 18.1174 34.1739 17.4264C36.9144 16.7722 39.9967 15.2331 41.3563 14.1648L24.8486 40.6391C24.4571 41.267 23.5429 41.267 23.1514 40.6391L6.64374 14.1648C8.00331 15.2331 11.0856 16.7722 13.8261 17.4264Z" />
              </svg>
            </div>
            <h2 className="text-lg font-bold leading-tight tracking-tight">MeetPro</h2>
          </div>
          <nav className="hidden md:flex gap-6">
            <Link to="/" className="text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-primary transition-colors">Dashboard</Link>
            <Link to="/schedule" className="text-sm font-medium text-primary border-b-2 border-primary pb-0.5">Schedule</Link>
            <a className="text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-primary transition-colors" href="#">Calendar</a>
            <a className="text-sm font-medium text-slate-500 dark:text-slate-400 hover:text-primary transition-colors" href="#">Recordings</a>
          </nav>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex gap-2">
            <button className="flex items-center justify-center rounded-full size-10 bg-slate-100 dark:bg-[#283039] hover:bg-slate-200 dark:hover:bg-[#374151] transition-colors">
              <span className="material-symbols-outlined text-[20px]">notifications</span>
            </button>
            <button onClick={() => navigate('/settings')} className="flex items-center justify-center rounded-full size-10 bg-slate-100 dark:bg-[#283039] hover:bg-slate-200 dark:hover:bg-[#374151] transition-colors">
              <span className="material-symbols-outlined text-[20px]">settings</span>
            </button>
            <div className="bg-center bg-no-repeat bg-cover rounded-full size-10 ml-2 border-2 border-slate-200 dark:border-[#283039]" style={{ backgroundImage: 'url("https://picsum.photos/100")' }}></div>
          </div>
        </div>
      </header>

      {/* 2. Main Content Layout (Sidebar + Main Panel) */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar - Agenda & Calendar */}
        <aside className="w-full md:w-80 lg:w-[400px] shrink-0 flex flex-col border-r border-slate-200 dark:border-[#283039] bg-white dark:bg-[#111418] overflow-y-auto hidden md:flex">
          <div className="p-6 pb-2">
            <div className="flex items-center justify-between mb-4">
              <button className="text-slate-500 hover:text-white transition-colors"><span className="material-symbols-outlined">chevron_left</span></button>
              <p className="text-base font-bold text-slate-900 dark:text-white">October 2023</p>
              <button className="text-slate-500 hover:text-white transition-colors"><span className="material-symbols-outlined">chevron_right</span></button>
            </div>
            <div className="grid grid-cols-7 gap-1 place-items-center opacity-80">
              {[...Array(31)].map((_, i) => (
                <button key={i} className={`size-8 rounded-full flex items-center justify-center text-sm ${i === 4 ? 'bg-primary text-white font-bold shadow-lg shadow-primary/30' : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-[#283039]'}`}>
                  {i + 1}
                </button>
              ))}
            </div>
          </div>

          <div className="h-px w-full bg-slate-200 dark:bg-[#283039] my-2"></div>

          <div className="p-6 flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-slate-900 dark:text-white">Today's Agenda</h3>
              <button className="text-xs text-primary font-medium hover:underline">View All</button>
            </div>

            <div className="p-4 rounded-lg bg-primary/10 border border-primary/20 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="px-2 py-1 rounded text-[10px] font-bold uppercase bg-primary text-white">Now</span>
                <span className="text-xs text-primary font-medium">In 15m</span>
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-900 dark:text-white mb-1">Q4 Strategy Review</h4>
                <p className="text-xs text-slate-500 dark:text-slate-400">10:00 AM - 11:00 AM</p>
              </div>
              <button className="w-full mt-1 py-2 rounded-full bg-primary hover:bg-blue-600 text-white text-xs font-bold transition-colors">Join Now</button>
            </div>
          </div>
        </aside>

        {/* Main Form Panel */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 lg:p-12 flex justify-center">
          <div className="w-full max-w-3xl flex flex-col gap-8">
            <div className="flex flex-col gap-2">
              <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">Schedule Meeting</h1>
              <p className="text-slate-500 dark:text-slate-400">Create a new meeting link and invite participants directly.</p>
            </div>

            <div className="bg-white dark:bg-[#111418] rounded-lg border border-slate-200 dark:border-[#283039] p-6 md:p-8 shadow-sm">
              <form onSubmit={handleCreate} className="flex flex-col gap-8">
                {/* Title */}
                <div className="flex flex-col gap-2">
                  <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Meeting Title</label>
                  <input
                    type="text"
                    placeholder="e.g., Weekly Team Sync"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    className="w-full bg-slate-50 dark:bg-[#1C232C] border border-slate-200 dark:border-[#283039] rounded-lg px-4 py-3 text-slate-900 dark:text-white focus:ring-2 focus:ring-primary focus:border-transparent focus:outline-none transition-all"
                  />
                </div>

                {/* Date & Time Row */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Date</label>
                    <div className="relative">
                      <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">calendar_today</span>
                      <input
                        type="text"
                        value={formData.date}
                        readOnly
                        className="w-full bg-slate-50 dark:bg-[#1C232C] border border-slate-200 dark:border-[#283039] rounded-lg pl-10 pr-4 py-3 text-sm text-slate-900 dark:text-white"
                      />
                    </div>
                  </div>
                  <div className="flex flex-col gap-2">
                    <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Time</label>
                    <div className="relative">
                      <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">schedule</span>
                      <select
                        value={formData.time}
                        onChange={(e) => setFormData({ ...formData, time: e.target.value })}
                        className="w-full bg-slate-50 dark:bg-[#1C232C] border border-slate-200 dark:border-[#283039] rounded-lg pl-10 pr-10 py-3 text-sm text-slate-900 dark:text-white appearance-none focus:ring-2 focus:ring-primary focus:outline-none"
                      >
                        <option>10:00 AM</option>
                        <option>10:30 AM</option>
                        <option>11:00 AM</option>
                      </select>
                      <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">expand_more</span>
                    </div>
                  </div>
                </div>

                {/* Duration */}
                <div className="flex flex-col gap-3">
                  <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Duration</label>
                  <div className="flex flex-wrap gap-3">
                    {['15m', '30m', '45m', '1h', 'Custom'].map((dur) => (
                      <label key={dur} className="cursor-pointer">
                        <input
                          type="radio"
                          name="duration"
                          className="peer sr-only"
                          checked={formData.duration === dur}
                          onChange={() => setFormData({ ...formData, duration: dur })}
                        />
                        <div className="px-5 py-2 rounded-full border border-slate-200 dark:border-[#283039] bg-slate-50 dark:bg-[#1C232C] text-sm font-medium text-slate-600 dark:text-slate-400 peer-checked:bg-primary peer-checked:text-white peer-checked:border-primary transition-all">
                          {dur}
                        </div>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Participants */}
                <div className="flex flex-col gap-3">
                  <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Invite Participants</label>
                  <div className="flex flex-wrap gap-2 mb-1">
                    {formData.participants.map(email => (
                      <div key={email} className="flex items-center gap-2 bg-primary/10 border border-primary/20 text-primary px-3 py-1 rounded-full text-sm font-medium">
                        <span>{email}</span>
                        <button type="button" className="hover:text-red-500 transition-colors flex items-center"><span className="material-symbols-outlined text-[16px]">close</span></button>
                      </div>
                    ))}
                  </div>
                  <div className="relative flex items-center group">
                    <span className="material-symbols-outlined absolute left-3 text-slate-400 group-focus-within:text-primary transition-colors text-[20px]">person_add</span>
                    <input
                      type="email"
                      placeholder="Enter email addresses"
                      className="w-full bg-slate-50 dark:bg-[#1C232C] border border-slate-200 dark:border-[#283039] rounded-lg pl-10 pr-24 py-3 text-slate-900 dark:text-white focus:ring-2 focus:ring-primary focus:outline-none"
                    />
                    <button type="button" className="absolute right-2 px-4 py-1.5 bg-slate-200 dark:bg-[#283039] hover:bg-slate-300 dark:hover:bg-[#374151] text-xs font-bold rounded-full transition-colors text-slate-700 dark:text-white">Add</button>
                  </div>
                </div>

                {/* Actions */}
                <div className="pt-4 border-t border-slate-100 dark:border-[#283039] flex items-center justify-end gap-4">
                  <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="px-6 py-3 rounded-full text-sm font-bold text-slate-500 hover:bg-slate-100 dark:hover:bg-[#283039] transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex items-center gap-2 bg-primary hover:bg-blue-600 text-white px-8 py-3 rounded-full text-sm font-bold shadow-lg shadow-blue-500/20 transition-all hover:scale-[1.02] active:scale-[0.98]"
                  >
                    <span className="material-symbols-outlined text-[20px]">link</span>
                    Create & Generate Link
                  </button>
                </div>
              </form>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

export default SchedulePage;