import './App.css'
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './RouteTable';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';

function App() {
  return (
    <div id="app-wrapper" className="h-full w-full flex flex-col items-center py-8 px-4">
      <BrowserRouter>
        <ToastProvider>
          <AuthProvider>
            <AppRoutes />
          </AuthProvider>
        </ToastProvider>
      </BrowserRouter>
    </div>
  );
}

export default App;
