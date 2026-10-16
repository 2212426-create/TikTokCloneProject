import { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { TopHeader } from './components/layout/TopHeader';
import { Dashboard } from './pages/Dashboard';
import { Users } from './pages/Users';
import { Moderation } from './pages/Moderation';
import { StoreModeration } from './pages/StoreModeration';
import { Products } from './pages/Products';
import { Reports } from './pages/Reports';
import { Settings } from './pages/Settings';
import { Ads } from './pages/Ads';
import { LoginPage } from './pages/LoginPage';
import { useAuth } from './components/auth-provider';
import { Loader2 } from 'lucide-react';

export default function App() {
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-10 h-10 animate-spin text-primary mx-auto mb-4" />
          <p className="font-label text-sm text-on-surface-variant">Đang kiểm tra đăng nhập...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <LoginPage />;
  }

  const renderContent = () => {
    switch (currentTab) {
      case 'dashboard':
        return <Dashboard onNavigate={setCurrentTab} />;
      case 'users':
        return <Users />;
      case 'moderation':
        return (user.role === 'admin' || user.role === 'moderator') ? <Moderation /> : <Dashboard onNavigate={setCurrentTab} />;
      case 'stores':
        return (user.role === 'admin' || user.role === 'moderator') ? <StoreModeration /> : <Dashboard onNavigate={setCurrentTab} />;
      case 'products':
        return (user.role === 'admin' || user.role === 'moderator') ? <Products /> : <Dashboard onNavigate={setCurrentTab} />;
      case 'reports':
        return <Reports />;
      case 'ads':
        return user.role === 'admin' ? <Ads /> : <Dashboard onNavigate={setCurrentTab} />;
      case 'settings':
        return user.role === 'admin' ? <Settings /> : <Dashboard onNavigate={setCurrentTab} />;
      default:
        return <Dashboard onNavigate={setCurrentTab} />;
    }
  };

  return (
    <div className="flex bg-background min-h-screen">
      <Sidebar
        currentTab={currentTab}
        onTabChange={setCurrentTab}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
      />

      <div className="flex-1 flex flex-col md:ml-[280px] w-full min-h-screen">
        <TopHeader onMenuClick={() => setMobileOpen(true)} />

        <main className="flex-1 p-6 max-w-[1440px] mx-auto w-full">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}
