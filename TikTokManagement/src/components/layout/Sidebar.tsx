import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  BarChart,
  Settings,
  HelpCircle,
  LogOut,
  PlaySquare,
} from 'lucide-react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: (string | undefined | null | false)[]) {
  return twMerge(clsx(inputs));
}

interface SidebarProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
}

export function Sidebar({ currentTab, onTabChange }: SidebarProps) {
  const tabs = [
    { id: 'dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { id: 'users', label: 'Người dùng', icon: Users },
    { id: 'moderation', label: 'Kiểm duyệt', icon: ShieldCheck },
    { id: 'reports', label: 'Báo cáo', icon: BarChart },
    { id: 'settings', label: 'Cài đặt', icon: Settings },
  ];

  return (
    <aside className="fixed left-0 top-0 h-screen w-[280px] bg-surface-low border-r border-outline-variant/20 z-40 hidden md:flex flex-col">
      <div className="flex flex-col h-full py-6 px-3">
        <div className="flex items-center gap-3 px-4 mb-16">
          <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center shrink-0">
            <PlaySquare className="text-primary" fill="currentColor" />
          </div>
          <div>
            <h1 className="font-headline text-2xl font-bold text-primary">TopTop Admin</h1>
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
          <div className="px-1">
            <button className="w-full bg-primary text-on-primary py-3 rounded-lg font-label text-sm font-bold shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/50 transition-all">
              Nâng cấp hệ thống
            </button>
          </div>
          <div className="space-y-2 pt-4 border-t border-outline-variant/20">
            <button className="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:text-on-surface hover:bg-surface-high transition-colors duration-200 rounded-lg">
              <HelpCircle className="w-5 h-5" />
              <span className="font-label text-sm">Hỗ trợ</span>
            </button>
            <button className="w-full flex items-center gap-3 px-4 py-3 text-on-surface-variant hover:text-on-surface hover:bg-surface-high transition-colors duration-200 rounded-lg">
              <LogOut className="w-5 h-5" />
              <span className="font-label text-sm">Đăng xuất</span>
            </button>
          </div>
        </div>
      </div>
    </aside>
  );
}
