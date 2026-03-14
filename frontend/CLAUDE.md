# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev       # Start dev server (Vite HMR)
npm run build     # Type-check + production build
npm run lint      # Run ESLint
npm run preview   # Preview production build locally
```

## Architecture

React 19 + TypeScript SPA built with Vite. Routing via React Router DOM 7.

**Entry point flow:** `index.html` → `main.tsx` → `App.tsx` (BrowserRouter) → `RouteTable.tsx` → page components

**Route structure:** Routes are defined in `src/RouteTable.tsx`. Currently only one route (`/` → `DailyComposer`). Add new routes there.

**Pages:** Live in `src/pages/<PageName>/`. Each page has an `index.tsx` re-export and a `<PageName>.tsx` component file.

**Path alias:** `@src/*` resolves to `src/*` (configured in `tsconfig.json` and `vite.config.ts`).

**Styling:** Tailwind CSS 4 via Vite plugin. Global styles and Google Fonts in `src/index.css`.

**React Compiler** is enabled via `babel-plugin-react-compiler` — avoid manual `useMemo`/`useCallback` optimizations.
