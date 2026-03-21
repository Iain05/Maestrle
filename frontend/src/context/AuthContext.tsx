import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as apiLogin, register as apiRegister, type AuthResponse } from '@src/api/auth';
import { replayPendingGuesses, clearPendingGuesses } from '@src/utils/replayPendingGuesses';

interface AuthUser {
  username: string;
  email: string;
  totalPoints: number;
  role: string;
  streak: number;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  addPoints: (points: number, newStreak: number) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const TOKEN_KEY = 'cg_token';
const USER_KEY = 'cg_user';

function loadStored(): { user: AuthUser | null; token: string | null } {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const raw = localStorage.getItem(USER_KEY);
    const user = raw ? (JSON.parse(raw) as AuthUser) : null;
    return { token, user };
  } catch {
    return { token: null, user: null };
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const stored = loadStored();
  const [token, setToken] = useState<string | null>(stored.token);
  const [user, setUser] = useState<AuthUser | null>(stored.user);

  useEffect(() => {
    if (!stored.token) return;
    fetch('/api/auth/me', { headers: { Authorization: `Bearer ${stored.token}` } })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!data) return;
        const updated: AuthUser = { username: data.username, email: data.email, totalPoints: data.totalPoints, role: data.role ?? 'USER', streak: data.streak ?? 0 };
        localStorage.setItem(USER_KEY, JSON.stringify(updated));
        setUser(updated);
        if (data.token) {
          localStorage.setItem(TOKEN_KEY, data.token);
          setToken(data.token);
        }
      })
      .catch(() => {});
  }, []);

  async function persist(res: AuthResponse) {
    const u: AuthUser = { username: res.username, email: res.email, totalPoints: res.totalPoints, role: res.role ?? 'USER', streak: res.streak ?? 0 };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setToken(res.token);
    setUser(u);
    await replayPendingGuesses(res.token);
    window.location.reload();
  }

  async function login(email: string, password: string) {
    await persist(await apiLogin(email, password));
  }

  async function register(username: string, email: string, password: string) {
    await persist(await apiRegister(username, email, password));
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    clearPendingGuesses();
    window.location.reload();
  }

  function addPoints(points: number, newStreak: number) {
    if (!user) return;
    const updated = { ...user, totalPoints: user.totalPoints + points, streak: newStreak };
    localStorage.setItem(USER_KEY, JSON.stringify(updated));
    setUser(updated);
  }

  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, token, isAdmin, login, register, logout, addPoints }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
