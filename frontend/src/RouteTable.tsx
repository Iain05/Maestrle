import { Routes, Route } from 'react-router-dom';
import DailyComposer from './pages/DailyComposer';
import Leaderboard from './pages/Leaderboard';
import SubmitExcerpt from './pages/SubmitExcerpt';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DailyComposer />} />
      <Route path="/leaderboard" element={<Leaderboard />} />
      <Route path="/submit" element={<SubmitExcerpt />} />
    </Routes>
  );
}
