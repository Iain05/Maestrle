import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import PageLayout from '@src/components/PageLayout';
import { getDailyChallenge } from '@src/api/excerpt';
import { useAuth } from '@src/context/AuthContext';

interface ChallengeEntry {
  date: string;
  challengeNumber: number;
}

interface MonthGroup {
  monthKey: string;
  monthLabel: string;
  challenges: ChallengeEntry[];
}

function generatePastChallenges(todayDate: string, challengeNumber: number): ChallengeEntry[] {
  const today = new Date(`${todayDate}T12:00:00`);
  const challenges: ChallengeEntry[] = [];
  for (let i = 1; i < challengeNumber; i++) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    challenges.push({
      date: d.toISOString().split('T')[0],
      challengeNumber: challengeNumber - i,
    });
  }
  return challenges;
}

function groupByMonth(challenges: ChallengeEntry[]): MonthGroup[] {
  const groups = new Map<string, ChallengeEntry[]>();
  for (const c of challenges) {
    const key = c.date.slice(0, 7);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(c);
  }
  return Array.from(groups.entries()).map(([key, challenges]) => ({
    monthKey: key,
    monthLabel: new Date(`${key}-15T12:00:00`).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }),
    challenges,
  }));
}

function formatCardDate(dateStr: string) {
  const d = new Date(`${dateStr}T12:00:00`);
  return {
    dayName: d.toLocaleDateString('en-US', { weekday: 'short' }),
    day: d.getDate(),
    month: d.toLocaleDateString('en-US', { month: 'short' }),
  };
}

const PastChallenges: React.FC = () => {
  const { token } = useAuth();
  const [monthGroups, setMonthGroups] = useState<MonthGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    getDailyChallenge(token)
      .then(({ challengeNumber, date }) => {
        if (!challengeNumber || !date || challengeNumber <= 1) {
          setMonthGroups([]);
          return;
        }
        setMonthGroups(groupByMonth(generatePastChallenges(date, challengeNumber)));
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [token]);

  const backButton = (
    <div className="flex items-center gap-2">
      <Link
        to="/leaderboard"
        className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        🏆<span className="hidden sm:inline">Leaderboard</span>
      </Link>
      <Link
        to="/"
        className="flex items-center gap-2 px-3 py-2 bg-surface border border-border text-ink text-sm font-semibold rounded-xl shadow-sm hover:shadow-md hover:border-border-hover transition-all"
      >
        ← <span className="hidden sm:inline">Today's Challenge</span>
      </Link>
    </div>
  );

  return (
    <PageLayout
      leftSlot={backButton}
      title="Past Challenges"
      subtitle="Browse and play previous daily excerpts"
    >
      <main className="max-w-2xl w-full flex flex-col gap-8">
        {loading && (
          <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
            {Array.from({ length: 12 }).map((_, i) => (
              <div key={i} className="h-20 bg-surface border border-border rounded-2xl animate-pulse" />
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="text-center py-16 text-ink-muted">
            <p className="text-4xl mb-3">🎵</p>
            <p className="font-semibold text-ink">Couldn't load challenges</p>
            <p className="text-sm mt-1">Please try again later.</p>
          </div>
        )}

        {!loading && !error && monthGroups.length === 0 && (
          <div className="text-center py-16 text-ink-muted">
            <p className="text-4xl mb-3">🎼</p>
            <p className="font-semibold text-ink">No past challenges yet</p>
            <p className="text-sm mt-1">Come back tomorrow!</p>
          </div>
        )}

        {monthGroups.map(({ monthKey, monthLabel, challenges }) => (
          <section key={monthKey}>
            <h2 className="serif text-xl text-ink mb-3">{monthLabel}</h2>
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
              {challenges.map(({ date, challengeNumber }) => {
                const { dayName, day, month } = formatCardDate(date);
                return (
                  <Link
                    key={date}
                    to={`/${date}`}
                    className="flex flex-col items-center py-4 px-2 bg-surface border border-border rounded-2xl shadow-sm hover:shadow-md hover:border-border-hover transition-all text-center"
                  >
                    <span className="text-primary font-bold text-base leading-none mb-1.5">#{challengeNumber}</span>
                    <span className="text-ink font-semibold text-sm leading-tight">{dayName} {day}</span>
                    <span className="text-ink-muted text-xs">{month}</span>
                  </Link>
                );
              })}
            </div>
          </section>
        ))}
      </main>
    </PageLayout>
  );
};

export default PastChallenges;
