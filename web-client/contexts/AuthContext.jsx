import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    axios.defaults.withCredentials = true;

    useEffect(() => {
        const responseInterceptor = axios.interceptors.response.use(
            (response) => response,
            async (error) => {
                const originalRequest = error.config;

                // Nếu lỗi 401 (Unauthorized) và chưa từng thử lại request này
                if (error.response?.status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        // Gọi endpoint refresh token. 
                        // VÌ đã bật withCredentials=true, trình duyệt sẽ TỰ ĐỘNG gửi cookie RefreshToken đi.
                        // Backend cần đọc cookie này, kiểm tra và set lại cookie AccessToken mới.
                        await axios.post('http://localhost:8080/api/auth/refresh-token');

                        // Nếu refresh thành công (không lỗi), gọi lại request ban đầu.
                        // Trình duyệt cũng sẽ tự động gửi cookie AccessToken mới vừa nhận được.
                        return axios(originalRequest);
                    } catch (refreshError) {
                        // Nếu refresh thất bại (token hết hạn hẳn), logout ra ngoài
                        logout();
                        return Promise.reject(refreshError);
                    }
                }

                return Promise.reject(error);
            }
        );

        return () => {
            axios.interceptors.response.eject(responseInterceptor);
        };
    }, []);

    // Kiểm tra session khi reload trang
    useEffect(() => {
        const checkAuth = async () => {
            try {
                const res = await axios.get('http://localhost:8080/api/users/me');
                if (res.data.code === 1000) {
                    setUser(res.data.data);
                }
            } catch (error) {

                setUser(null);
            } finally {
                setLoading(false);
            }
        };
        checkAuth();
    }, []);

    const login = async (email, password) => {
        try {
            const res = await axios.post("http://localhost:8080/api/auth/login", { email, password });
            if (res.data.code === 1000) {
                // Server phải trả về user info trong body, và set cookie HttpOnly ở header Response
                setUser(res.data.data.user);
                return { success: true };
            }
            return { success: false, message: res.data.message };
        } catch (error) {
            return {
                success: false,
                message: error.response?.data?.message || "Lỗi kết nối"
            };
        }
    };

    const logout = async () => {
        try {
            // Gọi API logout để server xóa cookie
            await axios.post("http://localhost:8080/api/auth/logout");
        } catch (error) {
            console.error("Logout error", error);
        } finally {
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{ user, setUser, login, logout, isAuthenticated: !!user }}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
