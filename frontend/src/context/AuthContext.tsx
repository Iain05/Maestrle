import React, { createContext, useContext, useState, useEffect } from 'react';
import { login as apiLogin, register as apiRegister, type AuthResponse } from '@src/api/auth';

interface AuthUser {
  username: string;
  email: string;
  totalPoints: number;
}

interface AuthContextValue {
  user: AuthUser | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  addPoints: (points: number) => void;
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
        const updated: AuthUser = { username: data.username, email: data.email, totalPoints: data.totalPoints };
        localStorage.setItem(USER_KEY, JSON.stringify(updated));
        setUser(updated);
        if (data.token) {
          localStorage.setItem(TOKEN_KEY, data.token);
          setToken(data.token);
        }
      })
      .catch(() => {});
  }, []);

  function persist(res: AuthResponse) {
    const u: AuthUser = { username: res.username, email: res.email, totalPoints: res.totalPoints };
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setToken(res.token);
    setUser(u);
  }

  async function login(email: string, password: string) {
    persist(await apiLogin(email, password));
  }

  async function register(username: string, email: string, password: string) {
    persist(await apiRegister(username, email, password));
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    window.location.reload();
  }

  function addPoints(points: number) {
    if (!user) return;
    const updated = { ...user, totalPoints: user.totalPoints + points };
    localStorage.setItem(USER_KEY, JSON.stringify(updated));
    setUser(updated);
  }

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, addPoints }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
