import React from 'react';

const DailyComposer: React.FC = () => {
  return (
    <>
      <header className="text-center mb-8 wax-w-2xl w-full">
        <h1 id="game-title" className="serif text-4xl font-bold mb-2 text-slate-900">
          Daily Composer
        </h1>
        <p id="game-instructions" className="text-slate-600 italic">
          Identify who composed this musical excerpt!
        </p>
      </header>
    </>
  )
}

export default DailyComposer;
