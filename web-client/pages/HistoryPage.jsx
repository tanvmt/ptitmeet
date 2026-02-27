import React, { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "../components/DashboardLayout";
import { meetingService } from "../services/meetingService";

const HistoryPage = () => {
  const navigate = useNavigate();
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const [historyData, setHistoryData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [filterRole, setFilterRole] = useState("ALL"); // ALL, HOST, GUEST
  const [filterStatus, setFilterStatus] = useState("ALL"); // ALL, ACTIVE, SCHEDULED, FINISHED, CANCELED
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 6;

  useEffect(() => {
    fetchHistory();
  }, [currentPage, filterRole, filterStatus]);

  const fetchHistory = async () => {
    try {
      setIsLoading(true);
      const data = await meetingService.getHistory(currentPage, itemsPerPage, filterRole, filterStatus);
      setHistoryData(data); //{ content: [...], totalPages: X, totalElements: Y }
    } catch (error) {
      console.error("Lỗi khi tải lịch sử:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFilterChange = (type, value) => {
    if (type === 'ROLE') setFilterRole(value);
    if (type === 'STATUS') setFilterStatus(value);
    setCurrentPage(1);
  };

  const totalPages = Math.ceil(historyData.totalElements / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;

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
          <span className="px-3 py-1 bg-slate-500/10 text-slate-500 dark:text-slate-400 text-xs font-bold rounded-full">
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
          <span className="px-3 py-1 bg-green-500/10 text-green-600 dark:text-green-400 text-xs font-bold rounded-full animate-pulse">
            Live
          </span>
        );
      case "SCHEDULED":
        return (
          <span className="px-3 py-1 bg-blue-500/10 text-blue-600 dark:text-blue-400 text-xs font-bold rounded-full">
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
      <div className="p-4 md:p-8 lg:p-12 max-w-7xl mx-auto flex flex-col h-full">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-slate-900 dark:text-white tracking-tight">
              Meeting History
            </h1>
            <p className="text-slate-500 dark:text-slate-400 mt-1">
              Review and search for meetings you have participated in.
            </p>
          </div>

          {/* FILTER TOOLBAR */}
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-1.5">
              <select
                value={filterRole}
                onChange={(e) => handleFilterChange('ROLE', e.target.value)}
                className="bg-transparent text-sm font-medium outline-none cursor-pointer px-3 py-1.5 text-slate-700 dark:text-slate-200"
              >
                <option value="ALL" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">All Roles</option>
                <option value="HOST" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Hosted by me</option>
                <option value="GUEST" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Attended as Guest</option>
              </select>
            </div>

            <div className="flex items-center gap-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-1.5">
              <select
                value={filterStatus}
                onChange={(e) => handleFilterChange('STATUS', e.target.value)}
                className="bg-transparent text-sm font-medium outline-none cursor-pointer px-3 py-1.5 text-slate-700 dark:text-slate-200"
              >
                <option value="ALL" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">All Statuses</option>
                <option value="ACTIVE" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Live / Active</option>
                <option value="SCHEDULED" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Upcoming</option>
                <option value="FINISHED" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Finished</option>
                <option value="CANCELED" className="bg-white dark:bg-[#1c2127] text-slate-900 dark:text-white">Canceled</option>
              </select>
            </div>
          </div>
        </div>

        {/* MAIN CONTENT */}
        {isLoading ? (
          <div className="flex justify-center items-center flex-1 min-h-[400px]">
            <div className="size-10 border-4 border-primary/30 border-t-primary rounded-full animate-spin"></div>
          </div>
        ) : historyData.totalElements === 0 ? (
          <div className="bg-white dark:bg-slate-800/50 rounded-2xl border border-slate-200 dark:border-slate-700 p-12 text-center flex flex-col items-center justify-center flex-1 min-h-[400px]">
            <span className="material-symbols-outlined text-6xl text-slate-300 dark:text-slate-600 mb-4">
              search_off
            </span>
            <h3 className="text-xl font-bold text-slate-700 dark:text-white mb-2">
              No results found
            </h3>
            <p className="text-slate-500">
              Try changing the filters or create a new meeting.
            </p>
            {(filterRole !== "ALL" || filterStatus !== "ALL") && (
              <button
                onClick={() => {
                  setFilterRole("ALL");
                  setFilterStatus("ALL");
                }}
                className="mt-4 text-primary font-medium hover:underline"
              >
                Clear filters
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
              {historyData.content.map((meeting) => (
                <div
                  key={meeting.meetingCode}
                  className="bg-white dark:bg-slate-800/80 rounded-2xl border border-slate-200 dark:border-slate-700 p-6 hover:shadow-lg transition-all group flex flex-col h-full"
                >
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-2">
                      {meeting.host ? (
                        <span className="px-2.5 py-1 bg-orange-500/10 border border-orange-500/20 text-orange-600 dark:text-orange-400 text-[10px] uppercase font-black tracking-wider rounded">
                          Host
                        </span>
                      ) : (
                        <span className="px-2.5 py-1 bg-blue-500/10 border border-blue-500/20 text-blue-600 dark:text-blue-400 text-[10px] uppercase font-black tracking-wider rounded">
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

                  {meeting.status === "ACTIVE" ||
                  meeting.status === "SCHEDULED" ? (
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
                  ) : (
                    <button
                      disabled
                      className="w-full py-2.5 bg-slate-100 dark:bg-slate-700/50 text-slate-400 font-bold rounded-xl cursor-not-allowed flex items-center justify-center gap-2"
                    >
                      Ended
                    </button>
                  )}
                </div>
              ))}
            </div>

            {/* PAGINATION NAVIGATION */}
            {historyData.totalPages > 1 && (
              <div className="mt-auto flex items-center justify-between border-t border-slate-200 dark:border-slate-700 pt-6">
                <p className="text-sm text-slate-500 dark:text-slate-400">
                  Showing{" "}
                  <span className="font-bold text-slate-900 dark:text-white">
                    {indexOfFirstItem + 1}
                  </span>{" "}
                  to{" "}
                  <span className="font-bold text-slate-900 dark:text-white">
                    {Math.min(currentPage * itemsPerPage, historyData.totalElements)}
                  </span>{" "}
                  of{" "}
                  <span className="font-bold text-slate-900 dark:text-white">
                    {historyData.totalElements}
                  </span>{" "}
                  meetings
                </p>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() =>
                      setCurrentPage((prev) => Math.max(prev - 1, 1))
                    }
                    disabled={currentPage === 1}
                    className="p-2 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    <span className="material-symbols-outlined">
                      chevron_left
                    </span>
                  </button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: totalPages }).map((_, index) => (
                      <button
                        key={index}
                        onClick={() => setCurrentPage(index + 1)}
                        className={`size-10 rounded-lg text-sm font-bold transition-all ${
                          currentPage === index + 1
                            ? "bg-primary text-white shadow-md shadow-primary/30"
                            : "text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700"
                        }`}
                      >
                        {index + 1}
                      </button>
                    ))}
                  </div>

                  <button
                    onClick={() =>
                      setCurrentPage((prev) => Math.min(prev + 1, totalPages))
                    }
                    disabled={currentPage === totalPages}
                    className="p-2 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    <span className="material-symbols-outlined">
                      chevron_right
                    </span>
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  );
};

export default HistoryPage;
