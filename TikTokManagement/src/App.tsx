/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { TopHeader } from './components/layout/TopHeader';
import { Dashboard } from './pages/Dashboard';
import { Users } from './pages/Users';
import { Moderation } from './pages/Moderation';
import { Reports } from './pages/Reports';
import { Settings } from './pages/Settings';

export default function App() {
  const [currentTab, setCurrentTab] = useState('dashboard');

  const renderContent = () => {
    switch (currentTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'users':
        return <Users />;
      case 'moderation':
        return <Moderation />;
      case 'reports':
        return <Reports />;
      case 'settings':
        return <Settings />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="flex bg-background min-h-screen">
      <Sidebar currentTab={currentTab} onTabChange={setCurrentTab} />
      
      <div className="flex-1 flex flex-col md:ml-[280px] w-full min-h-screen">
        <TopHeader />
        
        <main className="flex-1 p-6 max-w-[1440px] mx-auto w-full">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
