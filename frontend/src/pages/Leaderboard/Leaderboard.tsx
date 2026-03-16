import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  getDailyLeaderboard,
  getAllTimeLeaderboard,
  type LeaderboardPage,
} from '@src/api/leaderboard';
import { useAuth } from '@src/context/AuthContext';
import AuthModal from '@src/components/AuthModal';

type Tab = 'daily' | 'all-time';

const PAGE_SIZE = 50;

const Leaderboard: React.FC = () => {
  const { user, logout } = useAuth();
  const [authModal, setAuthModal] = useState<'login' | 'register' | null>(null);
  const [tab, setTab] = useState<Tab>('daily');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<LeaderboardPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const fetch = tab === 'daily'
      ? getDailyLeaderboard(page, PAGE_SIZE)
      : getAllTimeLeaderboard(page, PAGE_SIZE);
    fetch
      .then(setData)
      .catch(() => setError('Could not load leaderboard.'))
      .finally(() => setLoading(false));
  }, [tab, page]);

  function switchTab(next: Tab) {
    if (next === tab) return;
    setTab(next);
    setPage(0);
    setData(null);
  }

  const startRank = (data?.number ?? 0) * PAGE_SIZE + 1;

  const backLink = (
    <Link
      to="/"
      className="px-3 py-2 bg-white border border-slate-200 text-slate-600 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 transition-all"
    >
      ← Back to game
    </Link>
  );

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

  return (
    <>
      {/* Mobile: nav row above header */}
      <nav className="sm:hidden w-full max-w-2xl flex justify-between items-center mb-6">
        {backLink}
        {authButtons}
      </nav>

      {/* Desktop: 3-col grid with title centred */}
      <div className="hidden sm:grid w-full grid-cols-[1fr_auto_1fr] items-start mb-8">
        <div className="flex justify-start">{backLink}</div>
        <header className="text-center px-6">
          <h1 className="serif text-4xl font-bold mb-2 text-slate-900">Leaderboard</h1>
          <p className="text-slate-600 italic">Who's been listening closest?</p>
        </header>
        <div className="flex justify-end">{authButtons}</div>
      </div>

      {/* Mobile: header below nav */}
      <header className="sm:hidden text-center mb-8 max-w-2xl w-full">
        <h1 className="serif text-4xl font-bold mb-2 text-slate-900">Leaderboard</h1>
        <p className="text-slate-600 italic">Who's been listening closest?</p>
      </header>

      {authModal && (
        <AuthModal initialMode={authModal} onClose={() => setAuthModal(null)} />
      )}

      <main className="max-w-xl w-full flex flex-col gap-6">
        {/* Tab switcher */}
        <div className="flex rounded-xl overflow-hidden border-2 border-slate-200">
          <button
            onClick={() => switchTab('daily')}
            className={`flex-1 py-2.5 font-semibold transition-colors ${
              tab === 'daily'
                ? 'bg-indigo-600 text-white'
                : 'bg-white text-slate-600 hover:bg-slate-50'
            }`}
          >
            Today
          </button>
          <button
            onClick={() => switchTab('all-time')}
            className={`flex-1 py-2.5 font-semibold transition-colors ${
              tab === 'all-time'
                ? 'bg-indigo-600 text-white'
                : 'bg-white text-slate-600 hover:bg-slate-50'
            }`}
          >
            All-Time
          </button>
        </div>

        {/* Entries */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
          {loading && (
            <p className="text-center text-slate-500 py-10">Loading...</p>
          )}
          {error && (
            <p className="text-center text-red-500 py-10">{error}</p>
          )}
          {!loading && !error && data?.content.length === 0 && (
            <p className="text-center text-slate-400 py-10 italic">
              No scores yet — be the first!
            </p>
          )}
          {!loading && !error && data && data.content.length > 0 && (
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 text-sm font-medium">
                  <th className="text-left py-3 px-5 w-10">#</th>
                  <th className="text-left py-3 px-5">Player</th>
                  <th className="text-right py-3 px-5">Points</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((entry, i) => {
                  const rank = startRank + i;
                  const isTop3 = rank <= 3;
                  return (
                    <tr
                      key={entry.username}
                      className="border-b border-slate-50 last:border-0 hover:bg-slate-50 transition-colors"
                    >
                      <td className="py-3.5 px-5 text-slate-400 font-medium text-sm">
                        <span className="inline-block w-6 text-center">
                          {rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : rank}
                        </span>
                      </td>
                      <td className={`py-3.5 px-5 font-semibold ${isTop3 ? 'text-slate-800' : 'text-slate-700'}`}>
                        {entry.username}
                      </td>
                      <td className="py-3.5 px-5 text-right font-bold text-indigo-600">
                        {entry.points}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <button
              onClick={() => setPage((p) => p - 1)}
              disabled={data.first}
              className="px-4 py-2 bg-white border border-slate-200 text-slate-600 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none transition-all"
            >
              ← Previous
            </button>
            <span className="text-slate-500 text-sm">
              Page {data.number + 1} of {data.totalPages}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last}
              className="px-4 py-2 bg-white border border-slate-200 text-slate-600 text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none transition-all"
            >
              Next →
            </button>
          </div>
        )}
      </main>
    </>
  );
};

export default Leaderboard;
