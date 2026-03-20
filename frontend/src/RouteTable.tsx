import { Routes, Route } from 'react-router-dom';
import DailyComposer from './pages/DailyComposer';
import Leaderboard from './pages/Leaderboard';
import SubmitExcerpt from './pages/SubmitExcerpt';
import AdminReview from './pages/AdminReview';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DailyComposer />} />
      <Route path="/leaderboard" element={<Leaderboard />} />
      <Route path="/submit" element={<SubmitExcerpt />} />
      <Route path="/admin" element={<AdminReview />} />
    </Routes>
  );
}
