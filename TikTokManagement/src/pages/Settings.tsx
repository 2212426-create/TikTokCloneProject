import { useState, useEffect } from 'react';
import { Filter, Download, Lock, Trash2, ShieldAlert, Eye, ShieldCheck, ChevronLeft, ChevronRight, RefreshCw, CheckCircle, Video, UserX } from 'lucide-react';
import { clsx } from 'clsx';
import { db, collection, onSnapshot, doc, updateDoc, addDoc, query, orderBy, Timestamp } from '../lib/firebase';
import type { AuditLog } from '../types';

const ACTION_LABELS: Record<string, { label: string; color: string }> = {
  'BAN_USER': { label: 'Khóa User', color: 'text-error' },
  'UNBAN_USER': { label: 'Mở khóa User', color: 'text-secondary-container' },
  'APPROVE_VIDEO': { label: 'Duyệt Video', color: 'text-secondary-container' },
  'REJECT_VIDEO': { label: 'Gỡ Video', color: 'text-error' },
  'CHANGE_ROLE': { label: 'Thay đổi Quyền', color: 'text-tertiary' },
  'WARN_USER': { label: 'Cảnh cáo User', color: 'text-tertiary-container' },
};

const ACTION_ICONS: Record<string, typeof Lock> = {
  'BAN_USER': Lock,
  'UNBAN_USER': CheckCircle,
  'APPROVE_VIDEO': Video,
  'REJECT_VIDEO': Trash2,
  'CHANGE_ROLE': ShieldAlert,
  'WARN_USER': UserX,
};

export function Settings() {
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [role, setRole] = useState('moderator');
  const [staffEmail, setStaffEmail] = useState('');
  const [granting, setGranting] = useState(false);

  useEffect(() => {
    const q = query(collection(db, 'audit_logs'), orderBy('createdAt', 'desc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const logs: AuditLog[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        logs.push({
          id: docSnap.id,
          adminId: data.adminId || '',
          action: data.action || '',
          targetId: data.targetId || '',
          details: data.details || {},
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
        });
      });
      setAuditLogs(logs);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleGrantRole = async () => {
    if (!staffEmail.trim()) {
      alert('Vui lòng nhập email hoặc ID nhân viên!');
      return;
    }
    
    setGranting(true);
    try {
      // Log the role grant action
      await addDoc(collection(db, 'audit_logs'), {
        adminId: 'admin_web',
        action: 'CHANGE_ROLE',
        targetId: staffEmail,
        details: { newRole: role, grantedTo: staffEmail },
        createdAt: Timestamp.now(),
      });
      alert(`Đã cấp quyền ${role} cho ${staffEmail} thành công!`);
      setStaffEmail('');
    } catch (err) {
      console.error('Error granting role:', err);
      alert('Lỗi khi cấp quyền!');
    } finally {
      setGranting(false);
    }
  };

  const formatDateTime = (ts: number) => {
    const date = new Date(ts);
    return {
      time: date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      date: date.toLocaleDateString('vi-VN'),
    };
  };

  const getAdminColor = (adminId: string) => {
    const colors = [
      'bg-primary-container/20 text-primary',
      'bg-secondary-container/20 text-secondary-container',
      'bg-tertiary-container/20 text-tertiary',
      'bg-error-container/20 text-error',
    ];
    let hash = 0;
    for (let i = 0; i < adminId.length; i++) hash = adminId.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  };

  return (
    <div className="space-y-6">
      <div className="mb-8">
        <h2 className="font-headline text-3xl md:text-5xl font-bold text-on-surface mb-2">Nhật ký hoạt động & Phân quyền</h2>
        <p className="font-body text-base text-on-surface-variant">Quản lý và giám sát mọi thao tác của ban quản trị hệ thống. Dữ liệu đồng bộ từ Firestore.</p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-8">
        {/* Audit Logs List */}
        <section className="xl:col-span-8 flex flex-col gap-4">
          <div className="flex justify-between items-end mb-2">
            <h3 className="font-headline text-2xl font-semibold text-on-surface">Audit Logs</h3>
            <div className="flex gap-2">
              <button className="flex items-center gap-2 px-3 py-1.5 bg-surface-low border border-outline-variant/20 rounded-md font-label text-sm text-on-surface-variant hover:text-on-surface hover:bg-surface-high transition-colors">
                <Filter className="w-4 h-4" /> Lọc
              </button>
              <button className="flex items-center gap-2 px-3 py-1.5 bg-surface-low border border-outline-variant/20 rounded-md font-label text-sm text-on-surface-variant hover:text-on-surface hover:bg-surface-high transition-colors">
                <Download className="w-4 h-4" /> Xuất
              </button>
            </div>
          </div>

          <div className="bg-surface-low border border-outline-variant/20 rounded-xl overflow-hidden backdrop-blur-md">
            <div className="grid grid-cols-12 gap-4 px-6 py-4 bg-surface/50 border-b border-outline-variant/10 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">
              <div className="col-span-3">Thời gian</div>
              <div className="col-span-4">Admin</div>
              <div className="col-span-4">Hành động</div>
              <div className="col-span-1 text-right">Chi tiết</div>
            </div>

            {loading ? (
              <div className="px-6 py-10 text-center text-on-surface-variant">
                <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2" />
                Đang tải audit logs...
              </div>
            ) : auditLogs.length === 0 ? (
              <div className="px-6 py-10 text-center text-on-surface-variant">
                Chưa có nhật ký hoạt động nào.
              </div>
            ) : (
              auditLogs.map((log) => {
                const { time, date } = formatDateTime(log.createdAt);
                const actionInfo = ACTION_LABELS[log.action] || { label: log.action, color: 'text-on-surface-variant' };
                const ActionIcon = ACTION_ICONS[log.action] || ShieldAlert;
                return (
                  <div key={log.id} className="grid grid-cols-12 gap-4 px-6 py-4 border-b border-outline-variant/5 hover:bg-surface-high/50 transition-colors group">
                    <div className="col-span-3 flex flex-col justify-center">
                      <span className="font-label text-sm text-on-surface">{time}</span>
                      <span className="font-label text-xs text-on-surface-variant">{date}</span>
                    </div>
                    
                    <div className="col-span-4 flex items-center gap-3">
                      <div className={clsx("w-8 h-8 rounded-full flex items-center justify-center font-label text-xs font-bold", getAdminColor(log.adminId))}>
                        {log.adminId.substring(0, 2).toUpperCase()}
                      </div>
                      <span className="font-label text-sm text-on-surface truncate">{log.adminId}</span>
                    </div>

                    <div className="col-span-4 flex items-center">
                      <div className="flex items-start gap-3">
                        <ActionIcon className={clsx("w-5 h-5 mt-0.5", actionInfo.color)} />
                        <div>
                          <p className="font-label text-sm text-on-surface">{actionInfo.label}</p>
                          <p className="font-label text-xs text-on-surface-variant truncate">
                            Target: {log.targetId}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="col-span-1 flex items-center justify-end">
                      <button className="text-on-surface-variant hover:text-primary transition-colors opacity-0 group-hover:opacity-100">
                        <Eye className="w-5 h-5" />
                      </button>
                    </div>
                  </div>
                );
              })
            )}
            
            <div className="px-6 py-4 flex justify-between items-center bg-surface/30 border-t border-outline-variant/10">
              <span className="font-label text-xs text-on-surface-variant">
                Hiển thị {auditLogs.length} bản ghi
              </span>
              <div className="flex gap-2">
                <button className="p-1 text-on-surface-variant hover:text-on-surface disabled:opacity-50" disabled>
                  <ChevronLeft className="w-5 h-5" />
                </button>
                <button className="p-1 text-on-surface-variant hover:text-on-surface disabled:opacity-50" disabled>
                  <ChevronRight className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </section>

        {/* Access Management */}
        <section className="xl:col-span-4 flex flex-col gap-4">
          <h3 className="font-headline text-2xl font-semibold text-on-surface mb-2">Phân quyền Truy cập</h3>
          
          <div className="bg-surface-high border border-outline-variant/20 rounded-xl p-6 shadow-lg relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-[2px] bg-gradient-to-r from-primary to-secondary-container opacity-70"></div>
            
            <div className="flex items-center gap-4 mb-6 pb-6 border-b border-outline-variant/10">
              <div className="w-12 h-12 rounded-lg bg-surface flex items-center justify-center border border-outline-variant/30 text-primary">
                <ShieldCheck className="w-7 h-7" />
              </div>
              <div>
                <h4 className="font-headline text-lg font-semibold text-on-surface">Nhân viên mới</h4>
                <p className="font-label text-xs text-on-surface-variant">Cấp quyền cho tài khoản quản trị</p>
              </div>
            </div>

            <form className="flex flex-col gap-6" onSubmit={(e) => { e.preventDefault(); handleGrantRole(); }}>
              <div className="flex flex-col gap-2">
                <label className="font-label text-xs text-on-surface-variant">Email hoặc ID Nhân viên</label>
                <input 
                  type="text" 
                  placeholder="Nhập email..." 
                  value={staffEmail}
                  onChange={(e) => setStaffEmail(e.target.value)}
                  className="bg-surface border-0 border-b border-outline-variant focus:border-primary focus:ring-0 px-2 py-3 font-label text-sm text-on-surface placeholder:text-on-surface-variant/50 w-full transition-colors outline-none" 
                />
              </div>

              <div className="flex flex-col gap-4 mt-2">
                <label className="font-label text-xs text-on-surface-variant">Chọn vai trò (Role)</label>
                
                <label className="flex items-start gap-3 cursor-pointer group">
                  <div className="relative flex items-center justify-center mt-0.5">
                    <input 
                      type="radio" 
                      name="role" 
                      value="admin"
                      checked={role === 'admin'}
                      onChange={() => setRole('admin')}
                      className="peer appearance-none w-4 h-4 rounded-full border border-outline-variant checked:border-primary checked:border-[4px] transition-all bg-surface" 
                    />
                  </div>
                  <div>
                    <p className="font-label text-sm text-on-surface group-hover:text-primary transition-colors">Super Admin</p>
                    <p className="font-label text-xs text-on-surface-variant mt-1">Toàn quyền hệ thống, bao gồm thay đổi cấu hình lõi.</p>
                  </div>
                </label>

                <label className="flex items-start gap-3 cursor-pointer group">
                  <div className="relative flex items-center justify-center mt-0.5">
                    <input 
                      type="radio" 
                      name="role" 
                      value="moderator"
                      checked={role === 'moderator'}
                      onChange={() => setRole('moderator')}
                      className="peer appearance-none w-4 h-4 rounded-full border border-outline-variant checked:border-primary checked:border-[4px] transition-all bg-surface" 
                    />
                  </div>
                  <div>
                    <p className="font-label text-sm text-on-surface group-hover:text-primary transition-colors">Moderator</p>
                    <p className="font-label text-xs text-on-surface-variant mt-1">Duyệt nội dung, khóa bình luận, xử lý báo cáo cơ bản.</p>
                  </div>
                </label>

                <label className="flex items-start gap-3 cursor-pointer group">
                  <div className="relative flex items-center justify-center mt-0.5">
                    <input 
                      type="radio" 
                      name="role" 
                      value="user"
                      checked={role === 'user'}
                      onChange={() => setRole('user')}
                      className="peer appearance-none w-4 h-4 rounded-full border border-outline-variant checked:border-primary checked:border-[4px] transition-all bg-surface" 
                    />
                  </div>
                  <div>
                    <p className="font-label text-sm text-on-surface group-hover:text-primary transition-colors">User (Viewer)</p>
                    <p className="font-label text-xs text-on-surface-variant mt-1">Chỉ xem báo cáo, xuất dữ liệu, không có quyền can thiệp.</p>
                  </div>
                </label>
              </div>

              <button 
                type="submit"
                disabled={granting}
                className="mt-4 w-full bg-primary hover:brightness-110 text-on-primary font-label text-sm py-3 rounded-lg transition-all shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/50 font-bold disabled:opacity-50"
              >
                {granting ? 'Đang xử lý...' : 'Cấp quyền ngay'}
              </button>
            </form>
          </div>
        </section>
      </div>
    </div>
  );
}
