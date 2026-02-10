
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface LoginPageProps {
  onLoginSuccess?: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess }) => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [email, setEmail] = useState('');
  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    // Mock API Login Call
    setTimeout(() => {
      console.log("Logged in with:", email);
      setIsLoading(false);
      onLoginSuccess();
      navigate('/');
    }, 1500);
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6">
      <div className="max-w-md w-full">
        <div className="text-center mb-10">
          <div className="size-16 bg-primary rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary/20 mx-auto mb-6">
            <span className="material-symbols-outlined text-4xl">videocam</span>
          </div>
          <h2 className="text-3xl font-black mb-2">Welcome back</h2>
          <p className="text-gray-400">Join your team in a few simple steps.</p>
        </div>

        <form className="space-y-4" onSubmit={handleLogin}>
          <div className="space-y-1">
            <label className="text-sm font-bold text-gray-400 ml-1">Email Address</label>
            <input 
              type="email" 
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full h-12 px-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all" 
              placeholder="name@company.com" 
            />
          </div>
          <div className="space-y-1">
            <div className="flex justify-between items-center px-1">
              <label className="text-sm font-bold text-gray-400">Password</label>
              <button type="button" className="text-sm font-bold text-primary hover:underline">Forgot?</button>
            </div>
            <input 
              type="password" 
              required
              className="w-full h-12 px-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all" 
              placeholder="••••••••" 
            />
          </div>
          <button 
            type="submit"
            disabled={isLoading}
            className="w-full h-14 bg-primary hover:bg-blue-600 text-white font-black rounded-xl shadow-lg shadow-primary/20 transition-all active:scale-[0.98] flex items-center justify-center"
          >
            {isLoading ? <div className="size-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div> : 'Sign In'}
          </button>
        </form>

        <div className="relative my-10 flex items-center">
          <div className="flex-grow border-t border-white/5"></div>
          <span className="flex-shrink-0 mx-4 text-xs font-bold text-gray-600 uppercase tracking-widest">Or continue with</span>
          <div className="flex-grow border-t border-white/5"></div>
        </div>

        <button 
          onClick={handleLogin}
          className="w-full h-14 bg-white hover:bg-gray-100 text-black font-bold rounded-xl flex items-center justify-center gap-3 transition-all active:scale-[0.98]"
        >
          <img src="https://www.gstatic.com/images/branding/product/1x/gsa_512dp.png" className="size-6" alt="Google" />
          Continue with Google
        </button>

        <p className="mt-10 text-center text-gray-400 text-sm">
          Don't have an account? <button onClick={() => navigate('/signup')} className="font-bold text-primary hover:underline">Sign up for free</button>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
