import { useState } from 'react';
import axios from 'axios';
import { Camera, Mail, User } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const UserProfile = () => {
  const { user, setUser } = useAuth()

  const [formData, setFormData] = useState({
    fullName: user.fullName,
    email: user.email,
    avatarUrl: user.avatarUrl,
    cover: "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?ixlib=rb-4.0.3&auto=format&fit=crop&w=1000&q=80"
  });
  const [loading, setLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      const payload = {
        fullName: formData.fullName,
        avatarUrl: formData.avatarUrl
      };

      const response = await axios.put('http://localhost:8080/api/users/profile', payload);

      if (response.data.code === 1000) {
        setSuccessMessage('Cập nhật thông tin thành công!');
        setUser(response.data.data);
      } else {
        setErrorMessage(response.data.message || 'Cập nhật thất bại');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      setErrorMessage(error.response?.data?.message || 'Có lỗi xảy ra khi cập nhật');
    } finally {
      setLoading(false);
    }
  };
  return (
    <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* --- Cover & Avatar Section --- */}
      <div className="relative mb-8 group">
        {/* Cover Image */}
        <div className="h-32 w-full rounded-xl overflow-hidden relative">
          <img
            src={formData.cover}
            alt="Cover"
            className="w-full h-full object-cover opacity-80 group-hover:opacity-100 transition-opacity"
          />
          <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors"></div>
          <button className="absolute top-2 right-2 p-1.5 bg-black/40 hover:bg-black/60 backdrop-blur-md rounded-full text-white transition-all opacity-0 group-hover:opacity-100">
            <Camera size={16} />
          </button>
        </div>

        {/* Avatar */}
        <div className="absolute -bottom-6 left-6">
          <div className="relative inline-block">
            <img
              src={formData.avatarUrl}
              alt="Avatar"
              className="w-20 h-20 rounded-full border-4 border-surface-dark shadow-xl object-cover"
            />
            <button className="absolute bottom-0 right-0 p-1.5 bg-primary text-white rounded-full hover:bg-blue-600 transition shadow-lg ring-4 ring-surface-dark">
              <Camera size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* --- Form Section --- */}
      <div className="mt-8 space-y-6">

        {/* Public Profile Group */}
        <div className="space-y-4">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Full Name */}
            <div className="space-y-1.5">
              <label className="text-xs text-gray-400 font-medium ml-1">Full Name</label>
              <div className="relative group focus-within:text-primary">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-500 group-focus-within:text-primary transition-colors">
                  <User size={16} />
                </div>
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  className="w-full h-10 pl-10 pr-3 bg-white/5 border border-white/10 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all"
                />
              </div>
            </div>

            {/* Email */}
            <div className="space-y-1.5">
              <label className="text-xs text-gray-400 font-medium ml-1">Email Address</label>
              <div className="relative group focus-within:text-primary">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-500 group-focus-within:text-primary transition-colors">
                  <Mail size={16} />
                </div>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  disabled
                  className="w-full h-10 pl-10 pr-3 bg-white/5 border border-white/10 rounded-lg text-sm text-gray-400 cursor-not-allowed focus:outline-none focus:ring-1 focus:ring-primary focus:border-primary transition-all"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Feedback Messages */}
        {successMessage && (
          <div className="p-3 bg-green-500/10 border border-green-500/20 text-green-500 rounded-lg text-sm">
            {successMessage}
          </div>
        )}
        {errorMessage && (
          <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-500 rounded-lg text-sm">
            {errorMessage}
          </div>
        )}

        {/* Save Button */}
        <div className="flex justify-end pt-4">
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-6 py-2.5 bg-primary hover:bg-blue-600 text-white font-bold rounded-xl shadow-lg shadow-primary/20 transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {loading ? (
              <>
                <div className="size-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                <span>Saving...</span>
              </>
            ) : (
              'Save Changes'
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default UserProfile;