import React, { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { meetingService } from "../services/meetingService";
import JoinMeetingModal from "../components/JoinMeetingModal";

// 2. Dữ liệu Mock
const RECENT_ACTIVITIES = [
  {
    id: "1",
    title: "Q3 Marketing Strategy",
    meetingId: "892-231-009",
    date: "Today",
    time: "10:00 AM - 11:30 AM",
    status: "Completed",
    icon: "group",
    iconColor:
      "text-blue-600 dark:text-blue-400 bg-blue-100 dark:bg-blue-900/30",
  },
  {
    id: "2",
    title: "1:1 with Manager",
    meetingId: "442-110-552",
    date: "Yesterday",
    time: "02:00 PM - 02:45 PM",
    status: "Ended",
    icon: "person",
    iconColor:
      "text-purple-600 dark:text-purple-400 bg-purple-100 dark:bg-purple-900/30",
  },
  {
    id: "3",
    title: "Client Kickoff",
    meetingId: "112-998-334",
    date: "Mon, Sep 12",
    time: "09:00 AM - 10:30 AM",
    status: "Completed",
    icon: "rocket_launch",
    iconColor:
      "text-orange-600 dark:text-orange-400 bg-orange-100 dark:bg-orange-900/30",
  },
];

const UPCOMING_MEETING = {
  title: "Weekly Sync",
  team: "Engineering & Design Teams",
  time: "11:00 AM - 12:00 PM",
  meetingId: "884-219-440",
  startsIn: "15m",
  participants: [
    "https://picsum.photos/200?random=6",
    "https://picsum.photos/200?random=7",
    "https://picsum.photos/200?random=8",
  ],
  totalParticipants: 8,
};

const DashboardPage = ({ onLogout }) => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [isLoading, setIsLoading] = useState(false);
  const [isJoinModalOpen, setIsJoinModalOpen] = useState(false);
  const [time, setTime] = useState("10:42 AM");

  const handleCreateMeeting = async () => {
    try {
      setIsLoading(true);
      const meeting = await meetingService.createInstantMeeting();
      await handleJoinMeeting(meeting.meetingCode);
    } catch (error) {
      alert(`Lỗi tạo phòng: ${error.response?.data?.message || error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  const handleJoinMeeting = async (code) => {
    if (!code) return alert("Vui lòng nhập mã phòng!");

    try {
      setIsLoading(true);
      const response = await meetingService.joinMeeting(code);

      setIsJoinModalOpen(false);

      if (response.status === "ALLOWED") {
        navigate(`/meeting/${code}`, {
          state: {
            token: response.token,
            role: response.role,
            serverUrl: response.serverUrl,
          },
        });
      } else if (response.status === "PENDING") {
        navigate(`/waiting-room/${code}`);
      } else {
        alert("Bạn không thể tham gia cuộc họp này.");
      }
    } catch (error) {
      alert(`Lỗi tham gia: ${error.response?.data?.message || error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  // Cập nhật thời gian thực (tùy chọn)
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

  return (
    <div className="bg-background-light dark:bg-slate-950 text-slate-900 dark:text-white h-screen flex overflow-hidden font-display">
      <JoinMeetingModal
        isOpen={isJoinModalOpen}
        onClose={() => setIsJoinModalOpen(false)}
        onJoin={(code) => handleJoinLogic(code)}
        isLoading={isLoading}
      />
      {/* Sidebar */}
      <aside className="w-64 bg-white dark:bg-[#111418] border-r border-slate-200 dark:border-slate-800 flex flex-col justify-between shrink-0 z-20">
        <div className="flex flex-col h-full">
          <div className="h-16 flex items-center px-6 border-b border-slate-200 dark:border-slate-800/50">
            <div className="flex items-center gap-3">
              <div className="bg-primary/20 p-1.5 rounded-lg text-primary">
                <span className="material-symbols-outlined text-2xl">
                  videocam
                </span>
              </div>
              <div>
                <h1 className="text-base font-bold leading-none tracking-tight">
                  PTIT-Meet
                </h1>
                <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                  Enterprise
                </span>
              </div>
            </div>
          </div>

          <nav className="flex-1 overflow-y-auto py-6 px-3 flex flex-col gap-1">
            <Link
              to="/"
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-primary/10 text-primary transition-all group"
            >
              <span
                className="material-symbols-outlined group-hover:scale-110 transition-transform"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                grid_view
              </span>
              <span className="text-sm font-medium">Dashboard</span>
            </Link>
            <Link
              to="/schedule"
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white transition-all group"
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">
                event_available
              </span>
              <span className="text-sm font-medium">My Meetings</span>
            </Link>
            <a
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all group"
              href="#"
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">
                cloud_upload
              </span>
              <span className="text-sm font-medium">Recordings</span>
            </a>
            <button
              onClick={() => navigate("/settings")}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-all group"
            >
              <span className="material-symbols-outlined group-hover:scale-110 transition-transform">
                settings
              </span>
              <span className="text-sm font-medium">Settings</span>
            </button>
            <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-800/50 px-3">
              <p className="px-3 text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                Support
              </p>
              <button
                onClick={onLogout}
                className="flex w-full items-center gap-3 px-3 py-2.5 rounded-xl text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 transition-all group text-left"
              >
                <span className="material-symbols-outlined group-hover:scale-110 transition-transform">
                  logout
                </span>
                <span className="text-sm font-medium">Sign Out</span>
              </button>
            </div>
          </nav>

          <div className="p-4 border-t border-slate-200 dark:border-slate-800/50">
            <button className="flex w-full items-center gap-3 p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors">
              <div className="relative">
                <div
                  className="size-10 rounded-full bg-slate-700 bg-cover border-2 border-slate-700"
                  style={{ backgroundImage: `url(${user.avatarUrl})` }}
                ></div>
                <span className="absolute bottom-0 right-0 size-3 bg-green-500 border-2 border-white dark:border-[#111418] rounded-full"></span>
              </div>
              <div className="flex-1 overflow-hidden text-left">
                <p className="text-sm font-semibold truncate">
                  {user.fullName}
                </p>
                <p className="text-xs text-slate-500 truncate">{user.email}</p>
              </div>
              <span className="material-symbols-outlined text-slate-400 text-[20px]">
                expand_more
              </span>
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col h-full relative overflow-hidden">
        <header className="h-16 bg-white/80 dark:bg-slate-950/80 backdrop-blur-md sticky top-0 z-10 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between px-8">
          <div className="flex items-center text-slate-500 dark:text-slate-400 text-sm">
            <span>Dashboard</span>
            <span className="material-symbols-outlined mx-2 text-[16px]">
              chevron_right
            </span>
            <span className="text-slate-900 dark:text-white font-medium">
              Overview
            </span>
          </div>
          <div className="flex items-center gap-4">
            <div className="relative hidden md:block">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 material-symbols-outlined text-slate-400 text-[20px]">
                search
              </span>
              <input
                className="pl-10 pr-4 py-2 bg-slate-100 dark:bg-slate-900 border-none rounded-lg text-sm w-64 focus:ring-2 focus:ring-primary transition-all placeholder:text-slate-500"
                placeholder="Search meetings..."
              />
            </div>
            <button className="relative p-2 text-slate-500 hover:text-primary transition-colors rounded-full hover:bg-slate-100 dark:hover:bg-slate-800">
              <span className="material-symbols-outlined">notifications</span>
              <span className="absolute top-2 right-2 size-2 bg-red-500 rounded-full border-2 border-white dark:border-slate-950"></span>
            </button>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto p-4 md:p-8">
          <div className="max-w-7xl mx-auto flex flex-col gap-8">
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

            {/* Quick Actions Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
              <div className="xl:col-span-2 flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xl font-bold">Recent Activity</h3>
                  <button className="text-sm text-primary font-medium hover:underline">
                    View All History
                  </button>
                </div>
                <div className="bg-white dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
                  <div className="overflow-x-auto">
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
                        {RECENT_ACTIVITIES.map((activity) => (
                          <tr
                            key={activity.id}
                            className="hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors"
                          >
                            <td className="px-6 py-4">
                              <div className="flex items-center gap-3">
                                <div
                                  className={`p-2 rounded-lg ${activity.iconColor}`}
                                >
                                  <span className="material-symbols-outlined text-xl">
                                    {activity.icon}
                                  </span>
                                </div>
                                <div>
                                  <p className="text-sm font-semibold">
                                    {activity.title}
                                  </p>
                                  <p className="text-xs text-slate-500">
                                    ID: {activity.meetingId}
                                  </p>
                                </div>
                              </div>
                            </td>
                            <td className="px-6 py-4 text-sm">
                              <p>{activity.date}</p>
                              <p className="text-xs text-slate-500">
                                {activity.time}
                              </p>
                            </td>
                            <td className="px-6 py-4">
                              <span
                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                  activity.status === "Completed"
                                    ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                                    : "bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-400"
                                }`}
                              >
                                {activity.status}
                              </span>
                            </td>
                            <td className="px-6 py-4 text-right">
                              <button
                                className={`text-sm font-medium px-3 py-1.5 rounded-lg transition-colors ${
                                  activity.status === "Completed"
                                    ? "bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600"
                                    : "bg-primary/10 text-primary hover:bg-primary hover:text-white"
                                }`}
                              >
                                {activity.status === "Completed"
                                  ? "View Recording"
                                  : "Rejoin"}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              {/* Right Column: Upcoming Meeting */}
              <div className="xl:col-span-1 flex flex-col gap-4">
                <h3 className="text-xl font-bold">Up Next</h3>
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
                      Starts in {UPCOMING_MEETING.startsIn}
                    </div>
                  </div>
                  <div className="p-5 flex flex-col flex-1">
                    <div className="flex-1">
                      <h4 className="text-2xl font-bold text-white mb-1">
                        {UPCOMING_MEETING.title}
                      </h4>
                      <p className="text-slate-400 text-sm mb-4">
                        {UPCOMING_MEETING.team}
                      </p>
                      <div className="flex items-center gap-2 mb-2 text-slate-300 text-sm">
                        <span className="material-symbols-outlined text-[18px]">
                          schedule
                        </span>
                        <span>{UPCOMING_MEETING.time}</span>
                      </div>
                      <div className="flex items-center gap-2 mb-6 text-slate-300 text-sm">
                        <span className="material-symbols-outlined text-[18px]">
                          videocam
                        </span>
                        <span>ID: {UPCOMING_MEETING.meetingId}</span>
                      </div>
                      <div className="flex -space-x-3 mb-6">
                        {UPCOMING_MEETING.participants.map((avatar, idx) => (
                          <div
                            key={idx}
                            className="size-8 rounded-full border-2 border-[#1c2127] bg-cover bg-center"
                            style={{ backgroundImage: `url(${avatar})` }}
                          ></div>
                        ))}
                        <div className="size-8 rounded-full border-2 border-[#1c2127] bg-slate-600 flex items-center justify-center text-xs font-medium text-white">
                          +
                          {UPCOMING_MEETING.totalParticipants -
                            UPCOMING_MEETING.participants.length}
                        </div>
                      </div>
                    </div>
                    <button
                      onClick={() =>
                        handleJoinLogic(UPCOMING_MEETING.meetingId)
                      }
                      disabled={isLoading}
                      className="w-full bg-primary hover:bg-blue-600 text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg flex items-center justify-center gap-2"
                    >
                      <span className="material-symbols-outlined">
                        video_call
                      </span>
                      Join Meeting Now
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default DashboardPage;
