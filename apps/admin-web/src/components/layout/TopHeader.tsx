import { useState, useRef, useEffect } from 'react';
import { Moon, Sun, Menu, LogOut, Shield } from 'lucide-react';
import { NotificationBell } from './NotificationBell';
import { useTheme } from '../theme-provider';
import { useAuth } from '../auth-provider';

interface TopHeaderProps {
  title?: string;
  onMenuClick?: () => void;
}

export function TopHeader({ title, onMenuClick }: TopHeaderProps) {
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
    const roleConfig: Record<string, { colors: string; label: string }> = {
      admin: { colors: 'bg-primary/10 text-primary border-primary/20', label: 'Admin' },
      moderator: { colors: 'bg-secondary/10 text-secondary border-secondary/20', label: 'Moderator' },
      viewer: { colors: 'bg-tertiary/10 text-tertiary border-tertiary/20', label: 'Viewer' },
    };
    const config = roleConfig[user.role];
    if (!config) return null;
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full font-label text-[10px] font-bold border ${config.colors}`}>
        <Shield className="w-3 h-3" />
        {config.label}
      </span>
    );
  };

  return (
    <header className="sticky top-0 z-30 w-full bg-surface/80 backdrop-blur-xl border-b border-outline-variant/20 shadow-sm transition-colors duration-200">
      <div className="flex justify-between items-center h-16 px-6">
        <div className="flex items-center gap-4 flex-1">
          <button
            onClick={onMenuClick}
            className="md:hidden text-on-surface-variant p-2 hover:bg-surface-highest/50 rounded-full transition-all"
          >
            <Menu className="w-5 h-5" />
          </button>
          {title && <h2 className="hidden md:block font-headline text-lg font-semibold text-on-surface">{title}</h2>}
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1 text-on-surface-variant">
            <NotificationBell />
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
