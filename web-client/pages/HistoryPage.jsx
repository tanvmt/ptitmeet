import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "../components/DashboardLayout";
import { meetingService } from "../services/meetingService";

const HistoryPage = () => {
  const navigate = useNavigate();
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    try {
      const data = await meetingService.getHistory();
      setHistory(data);
    } catch (error) {
      console.error("Lỗi khi tải lịch sử:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return "Không xác định";
    const date = new Date(dateString);
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "FINISHED":
        return (
          <span className="px-3 py-1 bg-green-500/10 text-green-500 text-xs font-bold rounded-full">
            Finished
          </span>
        );
      case "CANCELED":
        return (
          <span className="px-3 py-1 bg-red-500/10 text-red-500 text-xs font-bold rounded-full">
            Canceled
          </span>
        );
      case "ACTIVE":
        return (
          <span className="px-3 py-1 bg-blue-500/10 text-blue-500 text-xs font-bold rounded-full animate-pulse">
            Live
          </span>
        );
      case "SCHEDULED":
        return (
          <span className="px-3 py-1 bg-orange-500/10 text-orange-500 text-xs font-bold rounded-full">
            Upcoming
          </span>
        );
      default:
        return (
          <span className="px-3 py-1 bg-slate-500/10 text-slate-500 text-xs font-bold rounded-full">
            {status}
          </span>
        );
    }
  };

  return (
    <DashboardLayout>
      <div className="p-4 md:p-8 lg:p-12 max-w-6xl mx-auto">
        <div className="flex flex-col gap-2 mb-8">
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">
            Meeting History
          </h1>
          <p className="text-slate-500 dark:text-slate-400">
            Review the list of meetings you have created and participated in.
          </p>
        </div>

        {isLoading ? (
          <div className="flex justify-center items-center h-64">
            <div className="size-10 border-4 border-primary/30 border-t-primary rounded-full animate-spin"></div>
          </div>
        ) : history.length === 0 ? (
          <div className="bg-white dark:bg-[#111418] rounded-2xl border border-slate-200 dark:border-[#283039] p-12 text-center flex flex-col items-center justify-center">
            <span className="material-symbols-outlined text-6xl text-slate-300 dark:text-slate-600 mb-4">
              history
            </span>
            <h3 className="text-xl font-bold text-slate-700 dark:text-white mb-2">
              No Data
            </h3>
            <p className="text-slate-500">
              You haven't participated in any recent meetings.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {history.map((meeting) => (
              <div
                key={meeting.meetingCode}
                className="bg-white dark:bg-[#111418] rounded-2xl border border-slate-200 dark:border-[#283039] p-6 hover:shadow-lg transition-shadow group flex flex-col h-full"
              >
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-center gap-2">
                    {meeting.host ? (
                      <span className="px-2.5 py-1 bg-primary/10 border border-primary/20 text-primary text-[10px] uppercase font-black tracking-wider rounded">
                        Host
                      </span>
                    ) : (
                      <span className="px-2.5 py-1 bg-slate-500/10 border border-slate-500/20 text-slate-500 text-[10px] uppercase font-black tracking-wider rounded">
                        Guest
                      </span>
                    )}
                  </div>
                  {getStatusBadge(meeting.status)}
                </div>

                <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-2 line-clamp-1 group-hover:text-primary transition-colors">
                  {meeting.title || "Untitled Meeting"}
                </h3>

                <div className="space-y-2 mb-6 flex-grow">
                  <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
                    <span className="material-symbols-outlined text-[18px]">
                      calendar_today
                    </span>
                    <span>{formatDateTime(meeting.startTime)}</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400">
                    <span className="material-symbols-outlined text-[18px]">
                      key
                    </span>
                    <span className="font-mono">{meeting.meetingCode}</span>
                  </div>
                </div>

                {/* If the meeting is live or upcoming, show the Rejoin button */}
                {(meeting.status === "ACTIVE" ||
                  meeting.status === "SCHEDULED") && (
                  <button
                    onClick={() =>
                      navigate(`/waiting-room/${meeting.meetingCode}`)
                    }
                    className="w-full py-2.5 bg-primary/10 hover:bg-primary/20 text-primary font-bold rounded-xl transition-colors flex items-center justify-center gap-2"
                  >
                    <span className="material-symbols-outlined text-[18px]">
                      login
                    </span>
                    Rejoin
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default HistoryPage;
