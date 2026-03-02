import React from "react";
import { useParticipants, VideoTrack } from "@livekit/components-react";
import { Track } from "livekit-client";

const ParticipantGrid = ({ sidebarOpen }) => {
    const participants = useParticipants();

    return (
        <main className={`flex-grow w-full h-full p-4 transition-all duration-300 ${sidebarOpen ? "lg:pr-[320px]" : ""}`}>
            <div className="h-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 content-center max-w-7xl mx-auto">

                {participants.map((p) => {
                    const name = p.name || p.identity || "Unknown";
                    const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random`;

                    // Fix lỗi TrackRef của LiveKit tại đây
                    const cameraTrackRef = { participant: p, source: Track.Source.Camera };

                    return (
                        <div
                            key={p.sid}
                            className={`group relative aspect-video w-full bg-surface rounded-2xl overflow-hidden border-2 transition-all ${
                                p.isSpeaking ? "border-primary shadow-lg shadow-primary/20" : "border-transparent"
                            }`}
                        >
                            {p.isCameraEnabled ? (
                                <VideoTrack trackRef={cameraTrackRef} className="w-full h-full object-cover" />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-800 to-gray-900">
                                    <div className="size-20 rounded-full bg-surface border border-white/10 flex items-center justify-center text-white text-2xl font-black shadow-2xl">
                                        <img src={avatarUrl} alt={name} className="w-full h-full rounded-full object-cover" />
                                    </div>
                                </div>
                            )}

                            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-60"></div>

                            <div className="absolute bottom-4 left-4 flex items-center gap-2">
                <span className="text-xs font-bold text-white tracking-wide truncate max-w-[120px]">
                  {name} {p.isLocal && "(You)"}
                </span>
                                {!p.isMicrophoneEnabled && (
                                    <span className="material-symbols-outlined text-red-500 text-[16px]">mic_off</span>
                                )}
                            </div>

                            {p.isSpeaking && (
                                <div className="absolute bottom-4 right-4 flex items-end gap-0.5 h-3">
                                    <div className="w-1 bg-primary rounded-full animate-[pulse_1s_infinite] h-2"></div>
                                    <div className="w-1 bg-primary rounded-full animate-[pulse_1.2s_infinite] h-3"></div>
                                    <div className="w-1 bg-primary rounded-full animate-[pulse_0.8s_infinite] h-1.5"></div>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </main>
    );
};

export default ParticipantGrid;