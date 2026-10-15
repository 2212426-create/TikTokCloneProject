import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  Store,
  ShoppingBag,
  BarChart,
  Megaphone,
  Settings,
  LogOut,
  X,
} from 'lucide-react';
import { cn } from '../../lib/utils';
import { useAuth } from '../auth-provider';
import logoImg from '../../assets/logo.png';

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  mobileOpen: boolean;
  onMobileClose: () => void;
}

export function Sidebar({ currentTab, onTabChange, mobileOpen, onMobileClose }: SidebarProps) {
  const { user, logout } = useAuth();
  const role = user?.role || 'user';

  const tabs = [
    { id: 'dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'users', label: 'Người dùng', icon: Users },
    { id: 'moderation', label: 'Kiểm duyệt Video', icon: ShieldCheck, roles: ['admin', 'moderator'] },
    { id: 'stores', label: 'Kiểm duyệt Gian hàng', icon: Store, roles: ['admin', 'moderator'] },
    { id: 'products', label: 'Kiểm duyệt Sản phẩm', icon: ShoppingBag, roles: ['admin', 'moderator'] },
    { id: 'reports', label: 'Báo cáo', icon: BarChart },
    { id: 'ads', label: 'Quảng cáo', icon: Megaphone, roles: ['admin'] },
    { id: 'settings', label: 'Cài đặt', icon: Settings, roles: ['admin'] },
  ].filter(tab => !tab.roles || tab.roles.includes(role));

  const handleTabChange = (tabId: string) => {
    onTabChange(tabId);
    onMobileClose();
  };

  return (
    <>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm md:hidden"
          onClick={onMobileClose}
        />
      )}

      <aside className={cn(
        "fixed left-0 top-0 h-screen w-[280px] bg-surface-low border-r border-outline-variant/20 z-50 flex flex-col transition-transform duration-300",
        mobileOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
      )}>
        <div className="flex flex-col h-full py-6 px-3">
          <div className="flex items-center justify-between px-4 mb-16">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg overflow-hidden shrink-0">
                <img src={logoImg} alt="Nông Video Logo" className="w-full h-full object-cover" />
              </div>
              <div>
                <h1 className="font-headline text-2xl font-bold text-primary">Nông Video</h1>
                <p className="font-label text-xs text-on-surface-variant">Hệ thống quản trị</p>
              </div>
            </div>
            <button
              onClick={onMobileClose}
              className="md:hidden p-2 text-on-surface-variant hover:text-on-surface hover:bg-surface-high rounded-full transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <nav className="flex-1 space-y-2">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = currentTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => handleTabChange(tab.id)}
                  className={cn(
                    'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-150',
                    isActive
                      ? 'bg-primary/20 text-primary font-bold opacity-90'
                      : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-high'
                  )}
                >
                  <Icon className={cn('w-5 h-5', isActive && 'fill-current')} />
                  <span className="font-label text-sm">{tab.label}</span>
                </button>
              );
            })}
          </nav>

          <div className="mt-auto space-y-4">
            <div className="space-y-2 pt-4 border-t border-outline-variant/20">
              <button
                onClick={() => { logout(); onMobileClose(); }}
                className="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:text-error hover:bg-error/10 transition-colors duration-200 rounded-lg"
              >
                <LogOut className="w-5 h-5" />
                <span className="font-label text-sm">Đăng xuất</span>
              </button>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
