
import React from 'react';

import { useNavigate } from 'react-router-dom';

const SignUpPage: React.FC = () => {
  const navigate = useNavigate();
  const handleSignUp = () => {
    //Logic đăng ký tài khoản ở đây (gọi API, xác thực, v.v.)
    navigate('/waiting-room');
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-6 relative overflow-hidden">
      <div className="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] bg-secondary/10 rounded-full blur-[120px] pointer-events-none"></div>
      
      <div className="max-w-md w-full relative">
        <div className="text-center mb-8">
          <div className="size-16 bg-primary rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary/20 mx-auto mb-6">
            <span className="material-symbols-outlined text-4xl">person_add</span>
          </div>
          <h2 className="text-3xl font-black mb-2 tracking-tight">Create your account</h2>
          <p className="text-gray-400">Join 10,000+ teams collaborating daily.</p>
        </div>

        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); handleSignUp(); }}>
          <div className="space-y-1">
            <label className="text-sm font-bold text-gray-500 ml-1 uppercase tracking-wider text-[10px]">Full Name</label>
            <div className="relative group">
               <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-primary transition-colors">badge</span>
               <input 
                type="text" 
                required
                className="w-full h-14 pl-12 pr-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all placeholder:text-gray-600" 
                placeholder="Alex Morgan" 
              />
            </div>
          </div>
          <div className="space-y-1">
            <label className="text-sm font-bold text-gray-500 ml-1 uppercase tracking-wider text-[10px]">Email Address</label>
            <div className="relative group">
               <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-primary transition-colors">mail</span>
               <input 
                type="email" 
                required
                className="w-full h-14 pl-12 pr-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all placeholder:text-gray-600" 
                placeholder="name@company.com" 
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-sm font-bold text-gray-500 uppercase tracking-wider text-[10px]">Password</label>
              <input 
                type="password" 
                required
                className="w-full h-14 px-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all placeholder:text-gray-600" 
                placeholder="••••••••" 
              />
            </div>
            <div className="space-y-1">
              <label className="text-sm font-bold text-gray-500 uppercase tracking-wider text-[10px]">Confirm</label>
              <input 
                type="password" 
                required
                className="w-full h-14 px-4 rounded-xl bg-surface border border-white/10 focus:ring-2 focus:ring-primary focus:outline-none transition-all placeholder:text-gray-600" 
                placeholder="••••••••" 
              />
            </div>
          </div>
          
          <div className="flex items-center gap-2 px-1 py-2">
            <input type="checkbox" id="terms" className="size-4 rounded border-white/10 bg-surface text-primary focus:ring-primary" required />
            <label htmlFor="terms" className="text-xs text-gray-400">I agree to the <a href="#" className="text-primary hover:underline">Terms of Service</a> and <a href="#" className="text-primary hover:underline">Privacy Policy</a></label>
          </div>

          <button 
            type="submit"
            className="w-full h-14 bg-primary hover:bg-blue-600 text-white font-black rounded-xl shadow-lg shadow-primary/20 transition-all active:scale-[0.98] mt-2"
          >
            Create Account
          </button>
        </form>

        <div className="relative my-8 flex items-center">
          <div className="flex-grow border-t border-white/5"></div>
          <span className="flex-shrink-0 mx-4 text-[10px] font-bold text-gray-600 uppercase tracking-widest">Or sign up with</span>
          <div className="flex-grow border-t border-white/5"></div>
        </div>

        <button 
          onClick={handleSignUp} //Xử lý đăng ký với Google ở đây
          className="w-full h-14 bg-white hover:bg-gray-100 text-black font-bold rounded-xl flex items-center justify-center gap-3 transition-all active:scale-[0.98] shadow-lg shadow-white/5"
        >
          <img src="https://www.gstatic.com/images/branding/product/1x/gsa_512dp.png" className="size-6" alt="Google" />
          Continue with Google
        </button>

        <p className="mt-8 text-center text-gray-400 text-sm">
          Already have an account? <button onClick={() => navigate('/login')} className="font-bold text-primary hover:underline">Sign In</button>
        </p>
      </div>
    </div>
  );
};

export default SignUpPage;
