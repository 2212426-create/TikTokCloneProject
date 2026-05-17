import { useState, useRef, useEffect } from 'react';
import { Search, HelpCircle, Moon, Sun, Menu, LogOut, Shield } from 'lucide-react';
import { NotificationBell } from './NotificationBell';
import { useTheme } from '../theme-provider';
import { useAuth } from '../auth-provider';

interface TopHeaderProps {
  title?: string;
}

export function TopHeader({ title }: TopHeaderProps) {
  const { theme, setTheme } = useTheme();
  const { user, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const isDark = theme === "dark" || (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getRoleBadge = () => {
    if (!user) return null;
    const colors = user.role === 'admin'
      ? 'bg-primary/10 text-primary border-primary/20'
      : 'bg-secondary-container/10 text-secondary-container border-secondary-container/20';
    const label = user.role === 'admin' ? 'Admin' : 'Moderator';
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full font-label text-[10px] font-bold border ${colors}`}>
        <Shield className="w-3 h-3" />
        {label}
      </span>
    );
  };

  return (
    <header className="sticky top-0 z-30 w-full bg-surface/80 backdrop-blur-xl border-b border-outline-variant/20 shadow-sm transition-colors duration-200">
      <div className="flex justify-between items-center h-16 px-6">
        <div className="flex items-center gap-4 flex-1">
          <button className="md:hidden text-on-surface-variant p-2 hover:bg-surface-highest/50 rounded-full transition-all">
            <Menu className="w-5 h-5" />
          </button>

          <div className="hidden md:flex relative group w-full max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant group-focus-within:text-primary transition-colors" />
            <input
              type="text"
              placeholder="Tìm kiếm..."
              className="w-full bg-surface-high border-b border-transparent focus:border-primary text-on-surface font-body text-sm pl-10 pr-4 py-2 outline-none transition-all placeholder:text-on-surface-variant/50 rounded-t-sm"
            />
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1 text-on-surface-variant">
            <NotificationBell />
            <button className="hidden md:flex p-2 hover:bg-surface-highest/50 rounded-full transition-all hover:text-on-surface">
              <HelpCircle className="w-5 h-5" />
            </button>
            <button
              onClick={() => setTheme(isDark ? "light" : "dark")}
              className="p-2 hover:bg-surface-highest/50 rounded-full transition-all hover:text-on-surface"
            >
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
          </div>

          <div className="h-6 w-px bg-outline-variant/30 hidden md:block"></div>

          {/* User Profile Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setShowDropdown(!showDropdown)}
              className="flex items-center gap-3 cursor-pointer hover:bg-surface-highest/50 p-1.5 rounded-full transition-all md:pr-3"
            >
              <div className="w-8 h-8 rounded-full overflow-hidden border border-outline-variant/30">
                {user?.photoURL ? (
                  <img
                    src={user.photoURL}
                    alt="Avatar"
                    className="w-full h-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <div className="w-full h-full bg-primary/20 flex items-center justify-center text-primary font-bold text-sm">
                    {user?.displayName?.charAt(0) || 'A'}
                  </div>
                )}
              </div>
              <div className="hidden md:flex flex-col items-start">
                <span className="font-label text-sm font-medium text-on-surface leading-tight">
                  {user?.displayName || 'Admin'}
                </span>
                {getRoleBadge()}
              </div>
            </button>

            {/* Dropdown Menu */}
            {showDropdown && (
              <div className="absolute right-0 top-full mt-2 w-64 bg-surface-low border border-outline-variant/20 rounded-xl shadow-2xl overflow-hidden z-50">
                <div className="p-4 border-b border-outline-variant/10 bg-surface-high/30">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full overflow-hidden border border-outline-variant/30 flex-shrink-0">
                      {user?.photoURL ? (
                        <img src={user.photoURL} alt="Avatar" className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                      ) : (
                        <div className="w-full h-full bg-primary/20 flex items-center justify-center text-primary font-bold">
                          {user?.displayName?.charAt(0) || 'A'}
                        </div>
                      )}
                    </div>
                    <div className="min-w-0">
                      <p className="font-label text-sm font-medium text-on-surface truncate">{user?.displayName}</p>
                      <p className="font-label text-xs text-on-surface-variant truncate">{user?.email}</p>
                    </div>
                  </div>
                  <div className="mt-2">{getRoleBadge()}</div>
                </div>
                <div className="p-2">
                  <button
                    onClick={() => { setShowDropdown(false); logout(); }}
                    className="w-full flex items-center gap-3 px-3 py-2.5 text-error hover:bg-error/10 rounded-lg transition-colors font-label text-sm"
                  >
                    <LogOut className="w-4 h-4" />
                    Đăng xuất
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
