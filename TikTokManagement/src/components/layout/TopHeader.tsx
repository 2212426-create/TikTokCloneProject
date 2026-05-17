import { Search, Bell, HelpCircle, Moon, Sun, Menu } from 'lucide-react';
import { useTheme } from '../theme-provider';

interface TopHeaderProps {
  title?: string;
}

export function TopHeader({ title }: TopHeaderProps) {
  const { theme, setTheme } = useTheme();
  
  const isDark = theme === "dark" || (theme === "system" && window.matchMedia("(prefers-color-scheme: dark)").matches);

  return (
    <header className="sticky top-0 z-30 w-full bg-surface/80 backdrop-blur-xl border-b border-outline-variant/20 shadow-sm md:w-[calc(100%-280px)] md:ml-[280px] transition-colors duration-200">
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
            <button className="p-2 hover:bg-surface-highest/50 rounded-full transition-all hover:text-on-surface relative">
              <Bell className="w-5 h-5" />
              <span className="absolute top-2 right-2 w-2 h-2 bg-primary rounded-full animate-pulse"></span>
            </button>
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
          
          <div className="flex items-center gap-3 cursor-pointer hover:bg-surface-highest/50 p-1.5 rounded-full transition-all md:pr-3">
            <div className="w-8 h-8 rounded-full overflow-hidden border border-outline-variant/30">
              <img
                src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop"
                alt="Admin Avatar"
                className="w-full h-full object-cover"
              />
            </div>
            <span className="font-label text-sm font-medium hidden md:block text-on-surface">Nguyễn Admin</span>
          </div>
        </div>
      </div>
    </header>
  );
}
