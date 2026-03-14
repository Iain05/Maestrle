import './App.css'
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './RouteTable'

function App() {

  return (
    <>
      <div id="app-wrapper" className="h-full w-full flex flex-col items-center py-8 px-4">
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </div>
    </>
  )
}

export default App
