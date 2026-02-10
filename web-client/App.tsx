import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import WaitingRoomPage from './pages/WaitingRoomPage';
import MeetingPage from './pages/MeetingPage';
import SummaryPage from './pages/SummaryPage';
import SchedulePage from './pages/SchedulePage';
import SignUpPage from './pages/SignUpPage';
import DashboardPage from './pages/DashboardPage';
import SettingsPage from './pages/SettingsPage';
const App: React.FC = () => {
  // Trạng thái user có thể giữ ở đây hoặc chuyển sang Context/Redux sau này
  const [userName, setUserName] = useState<string>('em Tai sieu cap co bap');
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(false);
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-background text-white selection:bg-primary/30">
        <Routes>
          <Route path="/" element={isLoggedIn ? <Navigate to="/dashboard" /> : <LandingPage />} />
          <Route path="/login" element={<LoginPage onLoginSuccess={() => setIsLoggedIn(true)} />} />
          <Route path="/signup" element={<SignUpPage />} /> 
          
          {/* Waiting Room có thể cần truyền thêm ID phòng họp sau này */}
          <Route 
            path="/waiting-room" 
            element={isLoggedIn ? <WaitingRoomPage userName={userName} /> : <Navigate to="/login" />} 
          />
          
          <Route path="/meeting" element={<MeetingPage />} />
          <Route path="/summary" element={<SummaryPage />} />

          <Route 
            path="/dashboard" 
            element={isLoggedIn ? <DashboardPage onLogout={() => setIsLoggedIn(false)} /> : <Navigate to="/login" />} 
          />

          <Route 
            path="/schedule" 
            element={isLoggedIn ? <SchedulePage /> : <Navigate to="/login" />} 
          />

          <Route path="/settings" element={isLoggedIn ? <SettingsPage /> : <Navigate to="/login" />} />
          
          {/* Route mặc định: Chuyển về trang chủ nếu không tìm thấy */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
};

export default App;