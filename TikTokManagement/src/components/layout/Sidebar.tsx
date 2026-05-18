import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  BarChart,
  Settings,
  HelpCircle,
  LogOut,
} from 'lucide-react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { useAuth } from '../auth-provider';
import logoImg from '../../assets/logo.png';

function cn(...inputs: (string | undefined | null | false)[]) {
  return twMerge(clsx(inputs));
}

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
}

export function Sidebar({ currentTab, onTabChange }: SidebarProps) {
  const { user, logout } = useAuth();
  const role = user?.role || 'user';

  const tabs = [
    { id: 'dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'users', label: 'Người dùng', icon: Users },
    { id: 'moderation', label: 'Kiểm duyệt', icon: ShieldCheck, roles: ['admin', 'moderator'] },
    { id: 'reports', label: 'Báo cáo', icon: BarChart },
    { id: 'settings', label: 'Cài đặt', icon: Settings, roles: ['admin'] },
  ].filter(tab => !tab.roles || tab.roles.includes(role));

  return (
    <aside className="fixed left-0 top-0 h-screen w-[280px] bg-surface-low border-r border-outline-variant/20 z-40 hidden md:flex flex-col">
      <div className="flex flex-col h-full py-6 px-3">
        <div className="flex items-center gap-3 px-4 mb-16">
          <div className="w-10 h-10 rounded-lg overflow-hidden shrink-0">
            <img src={logoImg} alt="TopTop Logo" className="w-full h-full object-cover" />
          </div>
          <div>
            <h1 className="font-headline text-2xl font-bold text-primary">TopTop</h1>
            <p className="font-label text-xs text-on-surface-variant">Hệ thống quản trị</p>
          </div>
        </div>

        <nav className="flex-1 space-y-2">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = currentTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
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
            <button className="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:text-on-surface hover:bg-surface-high transition-colors duration-200 rounded-lg">
              <HelpCircle className="w-5 h-5" />
              <span className="font-label text-sm">Hỗ trợ</span>
            </button>
            <button
              onClick={logout}
              className="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:text-error hover:bg-error/10 transition-colors duration-200 rounded-lg"
            >
              <LogOut className="w-5 h-5" />
              <span className="font-label text-sm">Đăng xuất</span>
            </button>
          </div>
        </div>
      </div>
    </aside>
  );
}
