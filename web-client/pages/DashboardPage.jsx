import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { meetingService } from "../services/meetingService";
import JoinMeetingModal from "../components/JoinMeetingModal";
import DashboardLayout from "../components/DashboardLayout";

const DashboardPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [isLoading, setIsLoading] = useState(false);
  const [isFetchingData, setIsFetchingData] = useState(true);
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [time, setTime] = useState(
    new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
  );

  const [recentActivities, setRecentActivities] = useState([]);
  const [upcomingMeeting, setUpcomingMeeting] = useState(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setIsFetchingData(true);

        const [historyData, upNextData] = await Promise.all([
          meetingService.getHistory(1, 5, "ALL", "ALL"),
          meetingService.getUpNext(),
        ]);

        setUpcomingMeeting(upNextData);

        let recent = historyData.content || [];
        if (upNextData) {
          recent = recent.filter(
            (m) => m.meetingCode !== upNextData.meetingCode
          );
        }

        setRecentActivities(recent);
      } catch (error) {
        console.error("Lỗi tải dữ liệu Dashboard:", error);
      } finally {
        setIsFetchingData(false);
      }
    };

    fetchDashboardData();
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      setTime(
        new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        })
      );
    }, 60000);
    return () => clearInterval(timer);
  }, []);

  const handleCreateMeeting = async () => {
    try {
      setIsLoading(true);
      const meeting = await meetingService.createInstantMeeting();
      const joinRes = await meetingService.joinMeeting(meeting.meetingCode);

      if (joinRes.status === "APPROVED") {
        navigate(`/meeting/${meeting.meetingCode}`, {
          state: {
            token: joinRes.token,
            role: joinRes.role,
            serverUrl: joinRes.serverUrl,
            micOn: true,
            camOn: true,
          },
        });
      }
    } catch (error) {
      alert(`Lỗi tạo phòng: ${error.response?.data?.message || error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleJoinMeeting = async (inputValue) => {
    if (!inputValue || inputValue.trim() === "") {
      return alert("Please enter a meeting code or link.");
    }

    let code = inputValue.trim();

    if (code.includes("/")) {
      if (code.endsWith("/")) {
        code = code.slice(0, -1);
      }
      code = code.split("/").pop();
    }

    code = code.replace(/\s+/g, "");
    setIsJoinModalOpen(false);
    navigate(`/waiting-room/${code}`);
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "N/A";
    const date = new Date(dateStr);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) return "Today";
    if (date.toDateString() === yesterday.toDateString()) return "Yesterday";
    return date.toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
    });
  };

  const formatTimeRange = (startStr, endStr) => {
    if (!startStr) return "N/A";
    const start = new Date(startStr).toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
    });
    const end = endStr
      ? new Date(endStr).toLocaleTimeString("en-US", {
          hour: "2-digit",
          minute: "2-digit",
        })
      : "TBD";
    return `${start} - ${end}`;
  };

  const getStartsInText = (startStr) => {
    if (!startStr) return "";
    const diffMs = new Date(startStr) - new Date();
    if (diffMs <= 0) return "Started";
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 60) return `${diffMins}m`;
    const diffHours = Math.floor(diffMins / 60);
    return `${diffHours}h ${diffMins % 60}m`;
  };

  const getIconData = (isHost) => {
    return isHost
      ? {
          icon: "star",
          color: "text-orange-600 bg-orange-100 dark:bg-orange-900/30",
        }
      : {
          icon: "group",
          color: "text-blue-600 bg-blue-100 dark:bg-blue-900/30",
        };
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "FINISHED":
        return "bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-400";
      case "ACTIVE":
        return "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400 animate-pulse";
      case "SCHEDULED":
        return "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400";
      case "CANCELED":
        return "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400";
      default:
        return "bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-400";
    }
  };

  return (
    <DashboardLayout>
      <JoinMeetingModal
        isOpen={isJoinModalOpen}
        onClose={() => setIsJoinModalOpen(false)}
        onJoin={(code) => handleJoinMeeting(code)}
        isLoading={isLoading}
      />

      <div className="flex-1 overflow-y-auto p-4 md:p-8">
        <div className="max-w-7xl mx-auto flex flex-col gap-8">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <h2 className="text-3xl md:text-4xl font-bold tracking-tight">
                Welcome back, {user.fullName}
              </h2>
              <p className="text-slate-500 dark:text-slate-400 mt-1">
                Here's what's happening with your meetings today.
              </p>
            </div>
            <div className="text-right hidden md:block">
              <p className="text-sm font-medium text-slate-500 uppercase tracking-wider">
                Current Time
              </p>
              <p className="text-xl font-mono font-semibold">{time}</p>
            </div>
          </div>

          {/* Quick Actions Grid (Giữ nguyên) */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/*  New Meeting, Join, Schedule  */}
            <button
              onClick={handleCreateMeeting}
              disabled={isLoading}
              className="relative group overflow-hidden rounded-xl p-6 flex flex-col justify-between h-40 bg-gradient-to-br from-[#137fec] to-[#0b5cbe] text-white shadow-lg shadow-blue-500/20 transition-all hover:shadow-blue-500/40 hover:-translate-y-1"
            >
              <div className="bg-white/20 p-2 rounded-lg backdrop-blur-sm w-fit">
                <span className="material-symbols-outlined text-3xl">
                  videocam
                </span>
              </div>
              <div className="text-left z-10">
                <h3 className="text-lg font-bold">
                  {isLoading ? "Creating..." : "New Meeting"}
                </h3>
                <p className="text-blue-100 text-sm">
                  Start an instant meeting
                </p>
              </div>
              <div className="absolute -right-6 -bottom-6 size-32 bg-white/10 rounded-full blur-2xl group-hover:scale-150 transition-transform duration-500"></div>
            </button>
            <button
              onClick={() => setIsJoinModalOpen(true)}
              className="relative group overflow-hidden rounded-xl p-6 flex flex-col justify-between h-40 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm transition-all hover:border-primary/50 hover:-translate-y-1"
            >
              <div className="bg-orange-100 dark:bg-orange-500/20 text-orange-600 dark:text-orange-400 p-2 rounded-lg w-fit">
                <span className="material-symbols-outlined text-3xl">
                  add_link
                </span>
              </div>
              <div className="text-left">
                <h3 className="text-lg font-bold">Join Meeting</h3>
                <p className="text-slate-500 text-sm">
                  Via ID or personal link
                </p>
              </div>
            </button>
            <button
              onClick={() => navigate("/schedule")}
              className="relative group overflow-hidden rounded-xl p-6 flex flex-col justify-between h-40 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 shadow-sm transition-all hover:border-primary/50 hover:-translate-y-1"
            >
              <div className="bg-purple-100 dark:bg-purple-500/20 text-purple-600 dark:text-purple-400 p-2 rounded-lg w-fit">
                <span className="material-symbols-outlined text-3xl">
                  calendar_month
                </span>
              </div>
              <div className="text-left">
                <h3 className="text-lg font-bold">Schedule</h3>
                <p className="text-slate-500 text-sm">Plan ahead for later</p>
              </div>
            </button>
          </div>

          {/* Main Content Split */}
          <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
            {/* Left Column: Recent Activity */}
            <div className="xl:col-span-2 flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <h3 className="text-xl font-bold">Recent Activity</h3>
                <button
                  onClick={() => navigate("/history")}
                  className="text-sm text-primary font-medium hover:underline"
                >
                  View All History
                </button>
              </div>
              <div className="bg-white dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
                <div className="overflow-x-auto min-h-[300px]">
                  {isFetchingData ? (
                    <div className="flex justify-center items-center h-full py-20">
                      <div className="size-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                    </div>
                  ) : recentActivities.length === 0 ? (
                    <div className="text-center py-20 text-slate-500">
                      No recent activities found.
                    </div>
                  ) : (
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="bg-slate-50 dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 text-xs uppercase text-slate-500 font-semibold tracking-wider">
                          <th className="px-6 py-4">Meeting Details</th>
                          <th className="px-6 py-4">Date & Time</th>
                          <th className="px-6 py-4">Status</th>
                          <th className="px-6 py-4 text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
                        {recentActivities.map((activity) => {
                          const iconData = getIconData(activity.host);
                          return (
                            <tr
                              key={activity.meetingCode}
                              className="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors"
                            >
                              <td className="px-6 py-4">
                                <div className="flex items-center gap-3">
                                  <div
                                    className={`p-2 rounded-lg ${iconData.color}`}
                                  >
                                    <span className="material-symbols-outlined text-xl">
                                      {iconData.icon}
                                    </span>
                                  </div>
                                  <div>
                                    <p className="text-sm font-semibold">
                                      {activity.title || "Untitled Meeting"}
                                    </p>
                                    <p className="text-xs text-slate-500">
                                      ID: {activity.meetingCode}
                                    </p>
                                  </div>
                                </div>
                              </td>
                              <td className="px-6 py-4 text-sm">
                                <p>{formatDate(activity.startTime)}</p>
                                <p className="text-xs text-slate-500">
                                  {formatTimeRange(
                                    activity.startTime,
                                    activity.endTime
                                  )}
                                </p>
                              </td>
                              <td className="px-6 py-4">
                                <span
                                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold tracking-wider ${getStatusBadge(
                                    activity.status
                                  )}`}
                                >
                                  {activity.status}
                                </span>
                              </td>
                              <td className="px-6 py-4 text-right">
                                <button
                                  onClick={() =>
                                    navigate(
                                      activity.status === "ACTIVE" ||
                                        activity.status === "SCHEDULED"
                                        ? `/waiting-room/${activity.meetingCode}`
                                        : `/history`
                                    )
                                  }
                                  className={`text-sm font-medium px-3 py-1.5 rounded-lg transition-colors ${
                                    activity.status === "FINISHED" ||
                                    activity.status === "CANCELED"
                                      ? "bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600"
                                      : "bg-primary/10 text-primary hover:bg-primary hover:text-white"
                                  }`}
                                >
                                  {activity.status === "FINISHED"
                                    ? "Details"
                                    : activity.status === "CANCELED"
                                    ? "View"
                                    : "Rejoin"}
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </div>

            {/* Right Column: Up Next */}
            <div className="xl:col-span-1 flex flex-col gap-4">
              <h3 className="text-xl font-bold">Up Next</h3>

              {isFetchingData ? (
                <div className="bg-[#1c2127] dark:bg-slate-800 rounded-xl border border-slate-700/50 shadow-lg relative overflow-hidden flex flex-col items-center justify-center min-h-[300px]">
                  <div className="size-8 border-4 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                </div>
              ) : upcomingMeeting ? (
                <div className="bg-[#1c2127] dark:bg-slate-800 rounded-xl border border-slate-700/50 shadow-lg relative overflow-hidden flex flex-col h-full min-h-[300px]">
                  <div
                    className="h-32 bg-cover bg-center relative"
                    style={{
                      backgroundImage:
                        "url('https://picsum.photos/400/200?random=5')",
                    }}
                  >
                    <div className="absolute inset-0 bg-gradient-to-t from-[#1c2127] dark:from-slate-800 to-transparent"></div>
                    <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full text-xs font-medium text-white flex items-center gap-1 border border-white/10">
                      <span className="block size-2 rounded-full bg-red-500 animate-pulse"></span>
                      {upcomingMeeting.status === "ACTIVE"
                        ? "Live Now"
                        : `Starts in ${getStartsInText(
                            upcomingMeeting.startTime
                          )}`}
                    </div>
                  </div>
                  <div className="p-5 flex flex-col flex-1">
                    <div className="flex-1">
                      <h4 className="text-2xl font-bold text-white mb-1">
                        {upcomingMeeting.title || "Untitled Meeting"}
                      </h4>
                      <p className="text-slate-400 text-sm mb-4">
                        {upcomingMeeting.host
                          ? "You are the Host"
                          : "You are a Guest"}
                      </p>
                      <div className="flex items-center gap-2 mb-2 text-slate-300 text-sm">
                        <span className="material-symbols-outlined text-[18px]">
                          schedule
                        </span>
                        <span>
                          {formatTimeRange(
                            upcomingMeeting.startTime,
                            upcomingMeeting.endTime
                          )}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 mb-6 text-slate-300 text-sm">
                        <span className="material-symbols-outlined text-[18px]">
                          videocam
                        </span>
                        <span>ID: {upcomingMeeting.meetingCode}</span>
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        handleJoinMeeting(upcomingMeeting.meetingCode)
                      }
                      disabled={isLoading}
                      className="w-full bg-primary hover:bg-blue-600 text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg flex items-center justify-center gap-2"
                    >
                      <span className="material-symbols-outlined">login</span>
                      Join Meeting Now
                    </button>
                  </div>
                </div>
              ) : (
                <div className="bg-[#1c2127] dark:bg-slate-800 rounded-xl border border-slate-700/50 shadow-lg relative overflow-hidden flex flex-col items-center justify-center text-center p-8 h-full min-h-[300px]">
                  <div className="size-16 rounded-full bg-slate-700/50 flex items-center justify-center mb-4 text-slate-400">
                    <span className="material-symbols-outlined text-3xl">
                      free_cancellation
                    </span>
                  </div>
                  <h4 className="text-lg font-bold text-white mb-2">
                    No Upcoming Meetings
                  </h4>
                  <p className="text-sm text-slate-400 mb-6">
                    You're all clear! Enjoy your free time or start a new
                    meeting.
                  </p>
                  <button
                    onClick={handleCreateMeeting}
                    className="text-sm font-bold text-primary hover:underline"
                  >
                    + Start an instant meeting
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default DashboardPage;
