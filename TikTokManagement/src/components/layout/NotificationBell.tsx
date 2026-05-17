import { useState, useEffect, useRef } from 'react';
import { Bell, X, Check, AlertTriangle, ShieldCheck, UserX, Video, Trash2 } from 'lucide-react';
import { db, collection, onSnapshot, doc, updateDoc, query, orderBy, limit } from '../../lib/firebase';
import { clsx } from 'clsx';

interface Notification {
  id: string;
  type: 'report' | 'moderation' | 'user_ban' | 'system';
  title: string;
  message: string;
  read: boolean;
  createdAt: number;
}

export function NotificationBell() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  // Listen to reports collection for new pending reports as notifications
  useEffect(() => {
    const q = query(collection(db, 'reports'), orderBy('createdAt', 'desc'), limit(20));
    const unsubReports = onSnapshot(q, (snapshot) => {
      const notifs: Notification[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        if (data.status === 'pending') {
          notifs.push({
            id: 'report_' + docSnap.id,
            type: 'report',
            title: `Báo cáo mới: ${data.reason || 'Vi phạm'}`,
            message: `${data.targetType === 'video' ? 'Video' : data.targetType === 'user' ? 'User' : 'Comment'} bị báo cáo: ${data.targetId || ''}`,
            read: false,
            createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
          });
        }
      });
      setNotifications((prev) => {
        // Merge report notifications with existing ones
        const nonReportNotifs = prev.filter((n) => !n.id.startsWith('report_'));
        return [...notifs, ...nonReportNotifs].sort((a, b) => b.createdAt - a.createdAt);
      });
    });

    // Listen to audit_logs for system activity
    const qLogs = query(collection(db, 'audit_logs'), orderBy('createdAt', 'desc'), limit(10));
    const unsubLogs = onSnapshot(qLogs, (snapshot) => {
      const logNotifs: Notification[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        const actionLabels: Record<string, string> = {
          'BAN_USER': 'User đã bị khóa',
          'UNBAN_USER': 'User đã được mở khóa',
          'APPROVE_VIDEO': 'Video đã được duyệt',
          'REJECT_VIDEO': 'Video đã bị gỡ',
          'CHANGE_ROLE': 'Quyền truy cập đã thay đổi',
        };
        logNotifs.push({
          id: 'log_' + docSnap.id,
          type: data.action?.includes('VIDEO') ? 'moderation' : data.action?.includes('USER') ? 'user_ban' : 'system',
          title: actionLabels[data.action] || data.action || 'Hoạt động hệ thống',
          message: `Bởi: ${data.adminId || 'system'} · Target: ${data.targetId || ''}`,
          read: true,
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
        });
      });
      setNotifications((prev) => {
        const nonLogNotifs = prev.filter((n) => !n.id.startsWith('log_'));
        return [...nonLogNotifs, ...logNotifs].sort((a, b) => b.createdAt - a.createdAt);
      });
    });

    return () => { unsubReports(); unsubLogs(); };
  }, []);

  // Close panel on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const formatTimeAgo = (ts: number) => {
    const diff = Date.now() - ts;
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (mins < 1) return 'Vừa xong';
    if (mins < 60) return `${mins}p`;
    if (hours < 24) return `${hours}h`;
    return `${days}d`;
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'report': return <AlertTriangle className="w-4 h-4 text-error" />;
      case 'moderation': return <Video className="w-4 h-4 text-secondary-container" />;
      case 'user_ban': return <UserX className="w-4 h-4 text-tertiary" />;
      default: return <ShieldCheck className="w-4 h-4 text-primary" />;
    }
  };

  return (
    <div className="relative" ref={panelRef}>
      <button
        onClick={() => setOpen(!open)}
        className="p-2 hover:bg-surface-highest/50 rounded-full transition-all hover:text-on-surface relative"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-error text-on-error rounded-full flex items-center justify-center font-label text-[10px] font-bold px-1 animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Notification Panel */}
      {open && (
        <div className="absolute right-0 top-full mt-2 w-96 max-h-[480px] bg-surface-low border border-outline-variant/20 rounded-xl shadow-2xl overflow-hidden z-50 flex flex-col">
          {/* Header */}
          <div className="p-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-high/30 flex-shrink-0">
            <h3 className="font-headline text-base font-semibold text-on-surface">Thông báo</h3>
            <div className="flex items-center gap-2">
              {unreadCount > 0 && (
                <button
                  onClick={markAllRead}
                  className="font-label text-xs text-primary hover:underline flex items-center gap-1"
                >
                  <Check className="w-3.5 h-3.5" />
                  Đọc tất cả
                </button>
              )}
              <button onClick={() => setOpen(false)} className="p-1 text-on-surface-variant hover:text-on-surface rounded">
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Notification List */}
          <div className="flex-1 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-8 text-center text-on-surface-variant">
                <Bell className="w-8 h-8 mx-auto mb-2 opacity-30" />
                <p className="font-label text-sm">Không có thông báo mới</p>
              </div>
            ) : (
              notifications.slice(0, 20).map((notif) => (
                <div
                  key={notif.id}
                  className={clsx(
                    'p-4 border-b border-outline-variant/5 hover:bg-surface-high/30 transition-colors cursor-pointer flex gap-3',
                    !notif.read && 'bg-primary/5 border-l-2 border-l-primary'
                  )}
                  onClick={() => {
                    setNotifications((prev) =>
                      prev.map((n) => n.id === notif.id ? { ...n, read: true } : n)
                    );
                  }}
                >
                  <div className="mt-0.5 flex-shrink-0">{getIcon(notif.type)}</div>
                  <div className="min-w-0 flex-1">
                    <div className="flex justify-between items-start gap-2">
                      <p className={clsx(
                        'font-label text-sm truncate',
                        !notif.read ? 'text-on-surface font-medium' : 'text-on-surface-variant'
                      )}>
                        {notif.title}
                      </p>
                      <span className="font-label text-[10px] text-on-surface-variant flex-shrink-0">
                        {formatTimeAgo(notif.createdAt)}
                      </span>
                    </div>
                    <p className="font-label text-xs text-on-surface-variant mt-0.5 truncate">{notif.message}</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
