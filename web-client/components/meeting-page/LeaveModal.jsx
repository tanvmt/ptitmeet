import React, { useState } from "react";

const LeaveModal = ({ isOpen, onClose, onConfirm, isHost, otherParticipants = [] }) => {
  const [step, setStep] = useState(1); // 1: Main choose, 2: Successor select
  const [selectedSuccessor, setSelectedSuccessor] = useState("");

  if (!isOpen) return null;

  const handleJustLeaveClick = () => {
    if (isHost && otherParticipants.length > 0) {
      setStep(2);
    } else {
      onConfirm("LEAVE");
    }
  };

  const handleConfirmSuccessor = () => {
    if (!selectedSuccessor) {
      alert("Please select a successor host first.");
      return;
    }
    // Confirm with transfer
    onConfirm("LEAVE", selectedSuccessor);
  };

  const handleClose = () => {
    setStep(1);
    setSelectedSuccessor("");
    onClose();
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="bg-surface border border-white/10 p-6 rounded-3xl shadow-2xl w-full max-w-sm flex flex-col gap-6 animate-in fade-in zoom-in duration-200 text-white">
        {step === 1 ? (
          <>
            <div>
              <h3 className="text-xl font-bold text-white mb-2">Leave meeting?</h3>
              {isHost ? (
                <p className="text-sm text-gray-400">
                  You are the host. Do you want to just leave, or end the meeting for everyone?
                </p>
              ) : (
                <p className="text-sm text-gray-400">
                  Are you sure you want to leave this meeting?
                </p>
              )}
            </div>
            
            <div className="flex flex-col gap-3">
              {isHost && (
                <button 
                  onClick={() => onConfirm("END")}
                  className="w-full py-3 rounded-xl bg-red-500 hover:bg-red-600 text-white font-bold transition-colors"
                >
                  End meeting for all
                </button>
              )}
              <button 
                onClick={handleJustLeaveClick}
                className="w-full py-3 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold transition-colors"
              >
                {isHost ? "Just leave" : "Leave Call"}
              </button>
              <button 
                onClick={handleClose}
                className="w-full py-3 rounded-xl bg-transparent text-gray-400 hover:text-white font-bold transition-colors mt-2"
              >
                Cancel
              </button>
            </div>
          </>
        ) : (
          <>
            <div>
              <h3 className="text-xl font-bold text-white mb-2">Assign successor host</h3>
              <p className="text-sm text-gray-400 mb-4">
                You must appoint a new Host before you leave the room.
              </p>
              
              <div className="max-h-48 overflow-y-auto flex flex-col gap-2 no-scrollbar border border-white/5 p-2 rounded-xl bg-background/25">
                {otherParticipants.map((p) => (
                  <label 
                    key={p.identity} 
                    className={`flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all border ${
                      selectedSuccessor === p.identity 
                        ? "bg-primary/25 border-primary text-white" 
                        : "bg-white/5 border-transparent text-gray-300 hover:bg-white/10"
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="size-8 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold text-xs">
                        {(p.name || p.identity || "U").charAt(0)}
                      </div>
                      <span className="text-sm font-semibold truncate max-w-[160px]">
                        {p.name || p.identity}
                      </span>
                    </div>
                    <input 
                      type="radio" 
                      name="successor" 
                      value={p.identity} 
                      checked={selectedSuccessor === p.identity}
                      onChange={() => setSelectedSuccessor(p.identity)}
                      className="accent-primary"
                    />
                  </label>
                ))}
              </div>
            </div>
            
            <div className="flex flex-col gap-2">
              <button 
                onClick={handleConfirmSuccessor}
                className="w-full py-3 rounded-xl bg-primary hover:bg-primary/90 text-white font-bold transition-colors"
              >
                Confirm & Leave
              </button>
              <button 
                onClick={() => setStep(1)}
                className="w-full py-3 rounded-xl bg-transparent text-gray-400 hover:text-white font-bold transition-colors"
              >
                Back
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default LeaveModal;