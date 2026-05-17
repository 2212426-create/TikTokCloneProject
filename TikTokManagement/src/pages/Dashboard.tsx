import { useState, useEffect } from 'react';
import { Users, Video, Eye, CircleSlash, TrendingUp, AlertTriangle, RefreshCw } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from 'recharts';
import { db, collection, onSnapshot, query, orderBy, limit, where, getCountFromServer } from '../lib/firebase';
import type { Report } from '../types';

export function Dashboard() {
  const [totalUsers, setTotalUsers] = useState(0);
  const [totalVideos, setTotalVideos] = useState(0);
  const [pendingReports, setPendingReports] = useState(0);
  const [recentReports, setRecentReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Count total users
    const usersRef = collection(db, 'users');
    const unsubUsers = onSnapshot(usersRef, (snapshot) => {
      setTotalUsers(snapshot.size);
    });

    // Count total videos
    const videosRef = collection(db, 'videos');
    const unsubVideos = onSnapshot(videosRef, (snapshot) => {
      setTotalVideos(snapshot.size);
    });

    // Count pending reports
    const reportsRef = collection(db, 'reports');
    const unsubReports = onSnapshot(reportsRef, (snapshot) => {
      let pending = 0;
      const reports: Report[] = [];
      snapshot.forEach((doc) => {
        const data = doc.data();
        if (data.status === 'pending') pending++;
        reports.push({
          id: doc.id,
          reporterId: data.reporterId || '',
          targetType: data.targetType || 'video',
          targetId: data.targetId || '',
          reason: data.reason || '',
          details: data.details || '',
          status: data.status || 'pending',
          createdAt: data.createdAt?.toMillis?.() || data.createdAt || Date.now(),
          handledBy: data.handledBy || '',
        });
      });
      setPendingReports(pending);
      // Sort newest first and take 5
      reports.sort((a, b) => b.createdAt - a.createdAt);
      setRecentReports(reports.slice(0, 5));
      setLoading(false);
    });

    return () => {
      unsubUsers();
      unsubVideos();
      unsubReports();
    };
  }, []);

  const formatNumber = (n: number) => {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'k';
    return n.toString();
  };

  const formatTimeAgo = (timestamp: number) => {
    const now = Date.now();
    const diff = now - timestamp;
    const mins = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (mins < 1) return 'Vừa xong';
    if (mins < 60) return `${mins}p trước`;
    if (hours < 24) return `${hours}h trước`;
    return `${days}d trước`;
  };

  const getReportColor = (reason: string) => {
    const lower = reason.toLowerCase();
    if (lower.includes('18+') || lower.includes('khỏa') || lower.includes('bạo lực')) return 'text-error border-error/50';
    if (lower.includes('spam')) return 'text-secondary-container border-secondary-container/50';
    if (lower.includes('bản quyền')) return 'text-tertiary border-tertiary/50';
    return 'text-on-surface-variant border-outline-variant/50';
  };

  // Static chart data for demo (would come from analytics in production)
  const chartData = [
    { name: 'T2', visits: 20, uploads: 10 },
    { name: 'T3', visits: 30, uploads: 12 },
    { name: 'T4', visits: 35, uploads: 15 },
    { name: 'T5', visits: 50, uploads: 20 },
    { name: 'T6', visits: 55, uploads: 35 },
    { name: 'T7', visits: 40, uploads: 30 },
    { name: 'CN', visits: 60, uploads: 35 },
  ];

  return (
    <div className="space-y-6">
      {/* Metrics Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-panel rounded-xl p-6 flex flex-col justify-between">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h3 className="font-label text-xs text-on-surface-variant uppercase tracking-wider">Tổng người dùng</h3>
              <div className="mt-2 flex items-baseline gap-3">
                <span className="font-headline text-4xl font-bold text-on-surface">{loading ? '...' : formatNumber(totalUsers)}</span>
              </div>
            </div>
            <div className="p-2 bg-surface-high rounded-lg text-primary">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="h-10 w-full bg-gradient-to-r from-primary/5 via-primary/20 to-primary/5 rounded-md mt-auto relative overflow-hidden">
            <svg className="absolute bottom-0 w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 100">
                <path d="M0 100 C 20 80, 40 90, 60 50 S 80 40, 100 20 L 100 100 Z" fill="var(--color-primary)" style={{ opacity: 0.1 }}></path>
                <path d="M0 100 C 20 80, 40 90, 60 50 S 80 40, 100 20" fill="none" stroke="var(--color-primary)" strokeWidth="2"></path>
            </svg>
          </div>
        </div>

        <div className="glass-panel rounded-xl p-6 flex flex-col justify-between">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h3 className="font-label text-xs text-on-surface-variant uppercase tracking-wider">Tổng số video</h3>
              <div className="mt-2 flex items-baseline gap-3">
                <span className="font-headline text-4xl font-bold text-on-surface">{loading ? '...' : formatNumber(totalVideos)}</span>
              </div>
            </div>
            <div className="p-2 bg-surface-high rounded-lg text-secondary">
              <Video className="w-5 h-5" />
            </div>
          </div>
          <div className="h-10 w-full bg-gradient-to-r from-secondary/5 via-secondary/20 to-secondary/5 rounded-md mt-auto relative overflow-hidden">
            <svg className="absolute bottom-0 w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 100">
                <path d="M0 100 C 30 70, 50 80, 70 30 S 90 20, 100 10 L 100 100 Z" fill="var(--color-secondary)" style={{ opacity: 0.1 }}></path>
                <path d="M0 100 C 30 70, 50 80, 70 30 S 90 20, 100 10" fill="none" stroke="var(--color-secondary)" strokeWidth="2"></path>
            </svg>
          </div>
        </div>

        <div className="bg-surface-high border border-outline-variant/30 rounded-xl p-6 flex flex-col justify-between">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h3 className="font-label text-xs text-on-surface-variant uppercase tracking-wider">Tổng báo cáo</h3>
              <div className="mt-2 flex items-baseline gap-3">
                <span className="font-headline text-4xl font-bold text-on-surface">{loading ? '...' : formatNumber(recentReports.length)}</span>
              </div>
            </div>
            <div className="p-2 bg-surface rounded-lg text-tertiary">
              <Eye className="w-5 h-5" />
            </div>
          </div>
          <div className="h-10 w-full bg-gradient-to-r from-tertiary/5 via-tertiary/20 to-tertiary/5 rounded-md mt-auto relative overflow-hidden">
            <svg className="absolute bottom-0 w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 100">
                <path d="M0 50 L 20 60 L 40 40 L 60 55 L 80 45 L 100 50 L 100 100 L 0 100 Z" fill="var(--color-tertiary)" style={{ opacity: 0.05 }}></path>
                <path d="M0 50 L 20 60 L 40 40 L 60 55 L 80 45 L 100 50" fill="none" stroke="var(--color-tertiary)" strokeWidth="2"></path>
            </svg>
          </div>
        </div>

        <div className="bg-surface-high border border-outline-variant/30 rounded-xl p-6 flex flex-col justify-between relative overflow-hidden">
          <div className="absolute top-0 right-0 w-24 h-24 bg-error/10 rounded-bl-full blur-2xl"></div>
          <div className="flex justify-between items-start mb-6 relative z-10">
            <div>
              <h3 className="font-label text-xs text-on-surface-variant uppercase tracking-wider">Chờ xử lý</h3>
              <div className="mt-2 flex items-baseline gap-3">
                <span className="font-headline text-4xl font-bold text-error">{loading ? '...' : pendingReports}</span>
              </div>
            </div>
            <div className="p-2 bg-error-container/30 rounded-lg text-error">
              <CircleSlash className="w-5 h-5" />
            </div>
          </div>
          <div className="flex items-center gap-2 mt-auto relative z-10">
            {pendingReports > 0 && <span className="w-2 h-2 rounded-full bg-error animate-pulse"></span>}
            <span className="font-label text-sm text-on-surface-variant">
              {pendingReports > 0 ? 'Cần xem xét ngay' : 'Không có báo cáo mới'}
            </span>
          </div>
        </div>
      </div>

      {/* Main Chart & Reports Area */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Chart Section */}
        <div className="lg:col-span-8 bg-surface-low border border-outline-variant/20 rounded-xl p-6">
          <div className="flex justify-between items-center mb-8">
            <h2 className="font-headline text-2xl font-semibold text-on-surface">Lưu lượng & Tải lên (7 ngày)</h2>
            <div className="flex gap-4">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-primary"></span>
                <span className="font-label text-sm text-on-surface-variant">Truy cập</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-sm bg-secondary"></span>
                <span className="font-label text-sm text-on-surface-variant">Tải lên</span>
              </div>
            </div>
          </div>
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 0, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorVisits" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorUploads" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--color-secondary)" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="var(--color-secondary)" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: 'var(--color-on-surface-variant)', fontSize: 12, fontFamily: 'Geist' }} dy={10} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: 'var(--color-on-surface-variant)', fontSize: 12, fontFamily: 'Geist' }} />
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--color-outline-variant)" strokeOpacity={0.4} />
                <Area type="monotone" dataKey="visits" stroke="var(--color-primary)" strokeWidth={3} fillOpacity={1} fill="url(#colorVisits)" />
                <Area type="monotone" dataKey="uploads" stroke="var(--color-secondary)" strokeWidth={2} strokeDasharray="4 4" fillOpacity={1} fill="url(#colorUploads)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Reports Section */}
        <div className="lg:col-span-4 bg-surface-low border border-outline-variant/20 rounded-xl flex flex-col overflow-hidden">
          <div className="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-high/50">
            <h2 className="font-headline text-xl text-on-surface flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-error" />
              Báo cáo vi phạm
            </h2>
            <span className="bg-error/20 text-error font-label text-xs px-3 py-1 rounded-full font-bold">
              {pendingReports} Mới
            </span>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-6 text-center text-on-surface-variant">
                <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2" />
                Đang tải...
              </div>
            ) : recentReports.length === 0 ? (
              <div className="p-6 text-center text-on-surface-variant">
                Không có báo cáo nào.
              </div>
            ) : (
              recentReports.map((report) => (
                <div key={report.id} className="p-5 border-b border-outline-variant/10 hover:bg-surface transition-colors cursor-pointer group">
                  <div className="flex justify-between items-start mb-2">
                    <span className={`font-label text-xs border px-2 py-0.5 rounded uppercase tracking-wider ${getReportColor(report.reason)}`}>
                      {report.reason}
                    </span>
                    <span className="font-label text-xs text-on-surface-variant">{formatTimeAgo(report.createdAt)}</span>
                  </div>
                  <p className="font-body text-base text-on-surface group-hover:text-primary transition-colors">
                    {report.targetType === 'video' ? 'Video' : report.targetType === 'user' ? 'User' : 'Comment'}: {report.targetId}
                  </p>
                  <p className="font-label text-xs text-on-surface-variant mt-1 line-clamp-2">{report.details}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <span className={`inline-flex items-center gap-1 text-xs font-label px-1.5 py-0.5 rounded ${
                      report.status === 'pending' ? 'bg-tertiary/10 text-tertiary-container' :
                      report.status === 'resolved' ? 'bg-secondary-container/10 text-secondary-container' :
                      'bg-surface-variant text-on-surface-variant'
                    }`}>
                      {report.status === 'pending' ? 'Chờ xử lý' : report.status === 'resolved' ? 'Đã xử lý' : 'Đã bỏ qua'}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
          <div className="p-4 text-center border-t border-outline-variant/10">
            <a href="#" className="font-label text-sm text-primary hover:underline">Xem tất cả báo cáo</a>
          </div>
        </div>
      </div>
    </div>
  );
}
