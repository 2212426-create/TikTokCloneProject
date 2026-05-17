import { useState, useEffect } from 'react';
import {
  Search,
  ChevronDown,
  RefreshCw,
  Eye,
  CheckCircle,
  XCircle,
  ChevronLeft,
  ChevronRight,
  Flag,
  AlertTriangle,
  MessageSquare,
  Video,
  User
} from 'lucide-react';
import { clsx } from 'clsx';
import { db, collection, onSnapshot, doc, updateDoc } from '../lib/firebase';
import type { Report, ReportStatus } from '../types';

export function Reports() {
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');

  useEffect(() => {
    const unsubscribe = onSnapshot(collection(db, 'reports'), (snapshot) => {
      const data: Report[] = [];
      snapshot.forEach((docSnap) => {
        const d = docSnap.data();
        data.push({
          id: docSnap.id,
          reporterId: d.reporterId || '',
          targetType: d.targetType || 'video',
          targetId: d.targetId || '',
          reason: d.reason || '',
          details: d.details || '',
          status: d.status || 'pending',
          createdAt: d.createdAt?.toMillis?.() || d.createdAt || Date.now(),
          handledBy: d.handledBy || '',
        });
      });
      data.sort((a, b) => b.createdAt - a.createdAt);
      setReports(data);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleResolve = async (reportId: string) => {
    try {
      await updateDoc(doc(db, 'reports', reportId), {
        status: 'resolved',
        handledBy: 'admin_web',
      });
    } catch (err) {
      console.error('Error resolving report:', err);
      alert('Lỗi khi xử lý báo cáo!');
    }
  };

  const handleDismiss = async (reportId: string) => {
    try {
      await updateDoc(doc(db, 'reports', reportId), {
        status: 'dismissed',
        handledBy: 'admin_web',
      });
    } catch (err) {
      console.error('Error dismissing report:', err);
      alert('Lỗi khi bỏ qua báo cáo!');
    }
  };

  const filteredReports = reports.filter((r) => {
    const matchSearch = r.reason.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        r.targetId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        r.reporterId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchStatus = statusFilter === 'all' || r.status === statusFilter;
    const matchType = typeFilter === 'all' || r.targetType === typeFilter;
    return matchSearch && matchStatus && matchType;
  });

  const pendingCount = reports.filter(r => r.status === 'pending').length;
  const resolvedCount = reports.filter(r => r.status === 'resolved').length;
  const dismissedCount = reports.filter(r => r.status === 'dismissed').length;

  const formatTimeAgo = (ts: number) => {
    const diff = Date.now() - ts;
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (mins < 1) return 'Vừa xong';
    if (mins < 60) return `${mins}p trước`;
    if (hours < 24) return `${hours}h trước`;
    return `${days} ngày trước`;
  };

  const getTargetIcon = (type: string) => {
    switch (type) {
      case 'video': return Video;
      case 'user': return User;
      case 'comment': return MessageSquare;
      default: return Flag;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-headline text-3xl font-bold text-on-surface mb-2">Báo cáo vi phạm</h2>
          <p className="text-on-surface-variant font-body text-base">
            Xử lý các báo cáo vi phạm từ người dùng. Đồng bộ real-time với Firestore collection <code className="text-primary">reports</code>.
          </p>
        </div>
        <div className="flex items-center gap-4 text-sm font-label">
          <span className="px-3 py-1 rounded-full bg-tertiary/10 text-tertiary-container border border-tertiary/20">
            ⏳ Chờ: {pendingCount}
          </span>
          <span className="px-3 py-1 rounded-full bg-secondary-container/10 text-secondary-container border border-secondary-container/20">
            ✅ Đã xử lý: {resolvedCount}
          </span>
          <span className="px-3 py-1 rounded-full bg-surface-variant text-on-surface-variant border border-outline-variant/20">
            ❌ Bỏ qua: {dismissedCount}
          </span>
        </div>
      </div>

      {/* Filters & Table */}
      <div className="bg-surface-low border border-outline-variant/20 rounded-xl overflow-hidden relative shadow-sm">
        <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-error/50 to-transparent"></div>

        {/* Toolbar */}
        <div className="p-6 border-b border-outline-variant/10 flex flex-col sm:flex-row gap-4 justify-between items-center bg-surface/30 backdrop-blur-md">
          <div className="relative w-full sm:max-w-xs group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-on-surface-variant group-focus-within:text-primary transition-colors" />
            <input
              type="text"
              placeholder="Tìm theo lý do, ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-surface border-b border-transparent focus:border-primary text-on-surface font-body text-base pl-10 pr-3 py-2 outline-none transition-all placeholder:text-on-surface-variant/50 rounded-t-sm"
            />
          </div>
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <div className="relative w-full sm:w-auto">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="appearance-none bg-surface border border-outline-variant/20 text-on-surface font-label text-sm py-2 pl-4 pr-10 rounded-lg outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all w-full"
              >
                <option value="all">Tất cả trạng thái</option>
                <option value="pending">Chờ xử lý</option>
                <option value="resolved">Đã xử lý</option>
                <option value="dismissed">Đã bỏ qua</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant pointer-events-none" />
            </div>
            <div className="relative w-full sm:w-auto">
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="appearance-none bg-surface border border-outline-variant/20 text-on-surface font-label text-sm py-2 pl-4 pr-10 rounded-lg outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all w-full"
              >
                <option value="all">Tất cả loại</option>
                <option value="video">Video</option>
                <option value="user">User</option>
                <option value="comment">Comment</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant pointer-events-none" />
            </div>
            <button className="p-2 border border-outline-variant/20 rounded-lg text-on-surface-variant hover:text-on-surface hover:bg-surface transition-colors">
              <RefreshCw className={clsx("w-5 h-5", loading && "animate-spin")} />
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr className="border-b border-outline-variant/10 bg-surface/50">
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Loại</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Lý do</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Mục tiêu</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Người báo cáo</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Thời gian</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Trạng thái</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/10">
              {loading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-on-surface-variant">
                    <RefreshCw className="w-5 h-5 animate-spin inline-block mr-2" />
                    Đang tải báo cáo...
                  </td>
                </tr>
              ) : filteredReports.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-on-surface-variant">Không tìm thấy báo cáo nào.</td>
                </tr>
              ) : filteredReports.map((report) => {
                const TargetIcon = getTargetIcon(report.targetType);
                return (
                  <tr key={report.id} className="hover:bg-surface-high/30 transition-colors group">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2">
                        <TargetIcon className="w-4 h-4 text-on-surface-variant" />
                        <span className="font-label text-xs uppercase">
                          {report.targetType === 'video' ? 'Video' : report.targetType === 'user' ? 'User' : 'Comment'}
                        </span>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 text-error flex-shrink-0" />
                        <span className="font-label text-sm text-on-surface">{report.reason}</span>
                      </div>
                      {report.details && (
                        <p className="font-label text-xs text-on-surface-variant mt-1 line-clamp-1">{report.details}</p>
                      )}
                    </td>
                    <td className="py-4 px-6 font-label text-sm text-on-surface-variant font-mono">{report.targetId}</td>
                    <td className="py-4 px-6 font-label text-sm text-on-surface-variant font-mono">{report.reporterId}</td>
                    <td className="py-4 px-6 font-label text-sm text-on-surface-variant">{formatTimeAgo(report.createdAt)}</td>
                    <td className="py-4 px-6">
                      {report.status === 'pending' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-tertiary/10 text-tertiary-container font-label text-xs border border-tertiary/30">
                          <span className="w-1.5 h-1.5 rounded-full bg-tertiary-container animate-pulse"></span>
                          Chờ xử lý
                        </span>
                      )}
                      {report.status === 'resolved' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-secondary-container/10 text-secondary-container font-label text-xs border border-secondary-container/20">
                          <CheckCircle className="w-3.5 h-3.5" />
                          Đã xử lý
                        </span>
                      )}
                      {report.status === 'dismissed' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-variant text-on-surface-variant font-label text-xs border border-outline-variant/20">
                          <XCircle className="w-3.5 h-3.5" />
                          Đã bỏ qua
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6 text-right">
                      {report.status === 'pending' && (
                        <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => handleResolve(report.id)}
                            className="p-1.5 text-on-surface-variant hover:text-secondary-container hover:bg-secondary-container/10 rounded transition-colors"
                            title="Đánh dấu đã xử lý"
                          >
                            <CheckCircle className="w-5 h-5" />
                          </button>
                          <button
                            onClick={() => handleDismiss(report.id)}
                            className="p-1.5 text-on-surface-variant hover:text-error hover:bg-error/10 rounded transition-colors"
                            title="Bỏ qua"
                          >
                            <XCircle className="w-5 h-5" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-6 border-t border-outline-variant/10 flex items-center justify-between bg-surface-lowest/50">
          <p className="font-label text-sm text-on-surface-variant">
            Hiển thị {filteredReports.length} của {reports.length} báo cáo
          </p>
          <div className="flex items-center gap-2">
            <button className="p-1.5 rounded border border-outline-variant/20 text-on-surface-variant hover:bg-surface-high transition-colors disabled:opacity-50" disabled>
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-1 px-2">
              <button className="w-8 h-8 rounded bg-primary-container/20 text-primary font-label text-sm font-bold flex items-center justify-center">1</button>
            </div>
            <button className="p-1.5 rounded border border-outline-variant/20 text-on-surface-variant hover:bg-surface-high transition-colors disabled:opacity-50" disabled>
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
