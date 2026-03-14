import { Routes, Route } from 'react-router-dom';

import DailyComposer from './pages/DailyComposer';

export const titleMap: Record<string, string> = {
  "/": "Daily Composer",
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<DailyComposer />} />
    </Routes>
  )
}
