import React, { useState } from 'react';
import { useAuth } from '@src/context/AuthContext';
import AuthModal from '@src/components/AuthModal';

interface PageLayoutProps {
  leftSlot: React.ReactNode;
  title: string;
  subtitle: string;
  children: React.ReactNode;
}

const PageLayout: React.FC<PageLayoutProps> = ({ leftSlot, title, subtitle, children }) => {
  const { user, logout } = useAuth();
  const [authModal, setAuthModal] = useState<'login' | 'register' | null>(null);

  const authButtons = (
    <div className="flex items-center gap-2">
      {user ? (
        <>
          <span className="text-slate-800 font-semibold">{user.username}</span>
          <span className="text-indigo-600 font-bold">{user.totalPoints} pts</span>
          <button
            onClick={logout}
            className="px-3 py-2 bg-white border border-slate-200 text-slate-600 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 transition-all"
          >
            Sign out
          </button>
        </>
      ) : (
        <>
          <button
            onClick={() => setAuthModal('login')}
            className="px-3 py-2 bg-white border border-slate-200 text-slate-700 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 transition-all"
          >
            Sign in
          </button>
          <button
            onClick={() => setAuthModal('register')}
            className="px-3 py-2 bg-indigo-600 text-white text-sm font-semibold rounded-xl shadow-sm hover:bg-indigo-700 hover:shadow-md transition-all"
          >
            <span className="sm:hidden">Register</span>
            <span className="hidden sm:inline">Create account</span>
          </button>
        </>
      )}
    </div>
  );

  const header = (
    <>
      <h1 className="serif text-4xl font-bold mb-2 text-slate-900">{title}</h1>
      <p className="text-slate-600 italic">{subtitle}</p>
    </>
  );

  return (
    <>
      {/* Mobile: nav row above header */}
      <nav className="sm:hidden w-full max-w-2xl flex justify-between items-center mb-6">
        {leftSlot}
        {authButtons}
      </nav>

      {/* Desktop: 3-col grid with title centred */}
      <div className="hidden sm:grid w-full grid-cols-[1fr_auto_1fr] items-start mb-8">
        <div className="flex justify-start">{leftSlot}</div>
        <header className="text-center px-6">{header}</header>
        <div className="flex justify-end">{authButtons}</div>
      </div>

      {/* Mobile: header below nav */}
      <header className="sm:hidden text-center mb-8 max-w-2xl w-full">{header}</header>

      {authModal && (
        <AuthModal initialMode={authModal} onClose={() => setAuthModal(null)} />
      )}

      {children}
    </>
  );
};

export default PageLayout;
