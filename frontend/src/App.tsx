import './App.css'
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './RouteTable';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { ThemeProvider } from './context/ThemeContext';

function App() {
  return (
    <ThemeProvider>
      <div id="app-wrapper" className="h-full w-full flex flex-col items-center pt-4 pb-8 px-4">
        <BrowserRouter>
          <ToastProvider>
            <AuthProvider>
              <AppRoutes />
            </AuthProvider>
          </ToastProvider>
        </BrowserRouter>
      </div>
      <a
        href="https://github.com/Iain05/Maestrle"
        target="_blank"
        rel="noopener noreferrer"
        className="fixed bottom-3 left-3 text-sm text-ink-muted hover:text-ink transition-colors"
      >
        v{__APP_VERSION__} · GitHub
      </a>
    </ThemeProvider>
  );
}

export default App;
