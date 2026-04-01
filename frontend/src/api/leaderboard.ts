export interface LeaderboardEntry {
  username: string;
  points: number;
  streak: number;
  totalPoints: number;
}

export interface LeaderboardPage {
  content: LeaderboardEntry[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export async function getDailyLeaderboard(page = 0, size = 10): Promise<LeaderboardPage> {
  const res = await fetch(`/api/leaderboard/daily?page=${page}&size=${size}`);
  if (!res.ok) throw new Error('Failed to fetch leaderboard');
  return res.json();
}

export async function getAllTimeLeaderboard(page = 0, size = 10): Promise<LeaderboardPage> {
  const res = await fetch(`/api/leaderboard/all-time?page=${page}&size=${size}`);
  if (!res.ok) throw new Error('Failed to fetch leaderboard');
  return res.json();
}

export interface MyRank {
  username: string;
  allTimeRank: number;
  allTimePoints: number;
  dailyRank: number | null;
  dailyPoints: number | null;
  streak: number;
}

export async function getMyRank(token: string): Promise<MyRank> {
  const res = await fetch('/api/leaderboard/my-rank', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('Failed to fetch rank');
  return res.json();
}
