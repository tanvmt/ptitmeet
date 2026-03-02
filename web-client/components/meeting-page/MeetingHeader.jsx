import React from "react";

const MeetingHeader = ({ code }) => (
    <header className="h-16 flex items-center justify-between px-6 bg-background/80 backdrop-blur-md z-30 shrink-0 border-b border-white/5">
        <div className="flex flex-col">
            <h1 className="text-sm font-bold text-white leading-none mb-1">Weekly Product Sync</h1>
            <span className="text-[10px] text-gray-500 font-bold uppercase tracking-widest">{code}</span>
        </div>
        <div className="flex items-center gap-2 bg-surface px-4 py-1.5 rounded-full border border-white/5">
            <span className="material-symbols-outlined text-green-500 text-[18px]">lock</span>
            <span className="text-[10px] font-bold text-gray-400 uppercase">Encrypted</span>
        </div>
    </header>
);

export default MeetingHeader;