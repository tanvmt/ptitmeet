import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const DashboardLayout = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (path) => location.pathname === path;

  return (
    <div className="bg-background-light dark:bg-slate-950 text-slate-900 dark:text-white h-screen flex overflow-hidden font-display">
      {/* 1. Sidebar dùng chung */}
      <aside className="w-64 bg-white dark:bg-[#111418] border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between shrink-0 z-20">
        <div className="flex flex-col h-full">
          <div className="h-16 flex items-center px-6 border-b border-slate-200 dark:border-slate-800/50">
            <div className="flex items-center gap-3">
              <div className="bg-primary/20 p-1.5 rounded-lg text-primary">
                <span className="material-symbols-outlined text-2xl">videocam</span>
              </div>
              <div>
                <h1 className="text-base font-bold leading-none tracking-tight">PTIT-Meet</h1>
                <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">Enterprise</span>
              </div>
            </div>
          </div>

          <nav className="flex-1 overflow-y-auto py-6 px-3 flex flex-col gap-1">
            <Link
              to="/dashboard"
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all group ${isActive('/dashboard') || isActive('/') ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform" style={isActive('/dashboard') ? { fontVariationSettings: "'FILL' 1" } : {}}>grid_view</span>
              <span className="text-sm font-medium">Dashboard</span>
            </Link>
            
            <Link
              to="/schedule"
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all group ${isActive('/schedule') ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'}`}
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform" style={isActive('/schedule') ? { fontVariationSettings: "'FILL' 1" } : {}}>event_available</span>
              <span className="text-sm font-medium">Schedule Meeting</span>
            </Link>

            <a className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all group" href="#">
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">cloud_upload</span>
              <span className="text-sm font-medium">Recordings</span>
            </a>
            
            <button onClick={() => navigate("/settings")} className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all group w-full ${isActive('/settings') ? 'bg-primary/10 text-primary' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'}`}>
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">settings</span>
              <span className="text-sm font-medium text-left">Settings</span>
            </button>

            <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-800/50 px-3">
              <p className="px-3 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Support</p>
              <button onClick={logout} className="flex w-full items-center gap-3 px-3 py-2.5 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 transition-all group text-left">
                <span className="material-symbols-outlined group-hover:scale-110 transition-transform">logout</span>
                <span className="text-sm font-medium">Sign Out</span>
              </button>
            </div>
          </nav>

          {/* User Info */}
          <div className="p-4 border-t border-slate-200 dark:border-slate-800/50">
            <button className="flex w-full items-center gap-3 p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              <div className="relative">
                <div className="size-10 rounded-full bg-slate-700 bg-cover border-2 border-slate-700" style={{ backgroundImage: `url(${user?.avatarUrl || 'https://picsum.photos/100'})` }}></div>
                <span className="absolute bottom-0 right-0 size-3 bg-green-500 border-2 border-white dark:border-[#111418] rounded-full"></span>
              </div>
              <div className="flex-1 overflow-hidden text-left">
                <p className="text-sm font-semibold truncate">{user?.fullName || 'User'}</p>
                <p className="text-xs text-slate-500 truncate">{user?.email}</p>
              </div>
            </button>
          </div>
        </div>
      </aside>

      {/* 2. Main Content Area */}
      <main className="flex-1 flex flex-col h-full relative overflow-hidden">
        {/* Header dùng chung */}
        <header className="h-16 bg-white/80 dark:bg-slate-950/80 backdrop-blur-md sticky top-0 z-10 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between px-8">
          <div className="flex items-center text-slate-500 dark:text-slate-400 text-sm">
            <span>PTIT-Meet</span>
            <span className="material-symbols-outlined mx-2 text-[16px]">chevron_right</span>
            <span className="text-slate-900 dark:text-white font-medium capitalize">
              {location.pathname === '/' ? 'Dashboard' : location.pathname.substring(1)}
            </span>
          </div>
          <div className="flex items-center gap-4">
            <div className="relative hidden md:block">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-[20px]">search</span>
              <input className="pl-10 pr-4 py-2 bg-slate-100 dark:bg-slate-900 border-none rounded-lg text-sm w-64 focus:ring-2 focus:ring-primary transition-all placeholder:text-slate-500" placeholder="Search..." />
            </div>
            <button className="relative p-2 text-slate-500 hover:text-primary transition-colors rounded-full hover:bg-slate-100 dark:hover:bg-slate-800">
              <span className="material-symbols-outlined">notifications</span>
            </button>
          </div>
        </header>

        {/* Nơi chèn nội dung của từng trang (Dashboard, Schedule...) */}
        <div className="flex-1 overflow-y-auto">
          {children}
        </div>
      </main>
    </div>
  );
};

export default DashboardLayout;