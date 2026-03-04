import React, { useMemo } from "react";
import { useTracks, VideoTrack } from "@livekit/components-react";
import { Track } from "livekit-client";

const ParticipantGrid = ({ sidebarOpen }) => {
    const tracks = useTracks([
        { source: Track.Source.Camera, withPlaceholder: true },
        { source: Track.Source.ScreenShare, withPlaceholder: false },
    ]);

    // 1. Tách biệt màn hình share và camera
    const screenShareTrack = tracks.find(t => t.source === Track.Source.ScreenShare);
    const cameraTracks = tracks.filter(t => t.source !== Track.Source.ScreenShare);

    // 2. Tính toán CSS cho Grid khi ở chế độ bình thường (không có share)
    const getGridClass = (count) => {
        if (count === 1) return "grid-cols-1";
        if (count <= 4) return "grid-cols-1 md:grid-cols-2";
        if (count <= 9) return "grid-cols-2 lg:grid-cols-3";
        return "grid-cols-2 md:grid-cols-3 lg:grid-cols-4";
    };

    return (
        <main className={`flex-grow w-full h-full p-4 transition-all duration-300 ${sidebarOpen ? "lg:pr-[320px]" : ""}`}>
            <div className="h-full max-w-[1600px] mx-auto flex flex-col lg:flex-row gap-4">

                {/* VÙNG HIỂN THỊ CHÍNH (SPOTLIGHT) */}
                <div className={`flex-grow flex items-center justify-center bg-black/20 rounded-3xl overflow-hidden min-h-[50vh] ${screenShareTrack ? 'lg:w-3/4' : 'w-full'}`}>
                    {screenShareTrack ? (
                        <ParticipantTile trackRef={screenShareTrack} isLarge />
                    ) : (
                        // Nếu không có share, hiển thị lưới camera
                        <div className={`grid gap-4 w-full h-full auto-rows-fr ${getGridClass(cameraTracks.length)}`}>
                            {cameraTracks.map((t) => (
                                <ParticipantTile key={`${t.participant.sid}-${t.source}`} trackRef={t} />
                            ))}
                        </div>
                    )}
                </div>

                {/* THANH SIDEBAR CHO CAMERA (Chỉ hiện khi đang có người share) */}
                {screenShareTrack && (
                    <div className="lg:w-1/4 flex lg:flex-col gap-3 overflow-x-auto lg:overflow-y-auto pb-2 lg:pb-0 scrollbar-hide">
                        {cameraTracks.map((t) => (
                            <div key={`${t.participant.sid}-${t.source}`} className="min-w-[200px] lg:min-w-full aspect-video">
                                <ParticipantTile trackRef={t} />
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </main>
    );
};

// Component con để tái sử dụng logic hiển thị
const ParticipantTile = ({ trackRef, isLarge = false }) => {
    const p = trackRef.participant;
    const isScreenShare = trackRef.source === Track.Source.ScreenShare;
    const name = p.name || p.identity || "Unknown";

    return (
        <div className={`group relative bg-surface rounded-2xl overflow-hidden border-2 transition-all shadow-lg w-full h-full ${
            p.isSpeaking && !isScreenShare ? "border-primary shadow-primary/20" : "border-white/5"
        }`}>
            {(p.isCameraEnabled || isScreenShare) ? (
                <VideoTrack trackRef={trackRef} className={`w-full h-full ${isScreenShare ? 'object-contain' : 'object-cover'} bg-black`} />
            ) : (
                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-800 to-black">
                    <img
                        src={`https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=random`}
                        alt={name}
                        className={`${isLarge ? 'size-32' : 'size-16'} rounded-full border-4 border-white/10`}
                    />
                </div>
            )}

            {/* Overlay Info */}
            <div className="absolute bottom-3 left-3 px-2 py-1 bg-black/40 backdrop-blur-md rounded-lg flex items-center gap-2">
                <span className="text-[11px] font-medium text-white">
                    {name} {p.isLocal && "(You)"} {isScreenShare && " đang trình bày"}
                </span>
                {!p.isMicrophoneEnabled && !isScreenShare && (
                    <span className="material-symbols-outlined text-red-500 text-[14px]">mic_off</span>
                )}
            </div>
        </div>
    );
};

export default ParticipantGrid;