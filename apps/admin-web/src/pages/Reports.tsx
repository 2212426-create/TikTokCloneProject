import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../components/auth-provider';
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
  User,
  Play,
  X,
  Loader2
} from 'lucide-react';
import { clsx } from 'clsx';
import { db, collection, onSnapshot, doc, updateDoc, getDoc } from '../lib/firebase';
import { formatTimeAgo } from '../lib/utils';
import type { Report, ReportStatus, Video as VideoData } from '../types';

export function Reports() {
  const { user } = useAuth();
  const adminName = user?.email || 'admin_unknown';
  
  const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [currentPage, setCurrentPage] = useState(1);
  const PAGE_SIZE = 10;

  const [playingVideo, setPlayingVideo] = useState<VideoData | null>(null);
  const [fetchingVideo, setFetchingVideo] = useState(false);

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
          appeal: d.appeal || '',
          status: d.status || 'pending',
          createdAt: d.timestamp?.toMillis?.() || d.timestamp || d.createdAt?.toMillis?.() || d.createdAt || Date.now(),
          handledBy: d.handledBy || '',
        });
      });
      data.sort((a, b) => b.createdAt - a.createdAt);
      setReports(data);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleResolve = async (report: Report) => {
    try {
      await updateDoc(doc(db, 'reports', report.id), {
        status: 'resolved',
        handledBy: adminName,
      });

      if (report.targetType === 'video') {
        try {
          await updateDoc(doc(db, 'videos', report.targetId), {
            moderationStatus: 'rejected',
            rejectedReason: `Bị gỡ do vi phạm: ${report.reason}`,
            reviewedBy: adminName,
          });
        } catch (videoErr) {
          console.error('Error auto-rejecting video:', videoErr);
        }
      }
    } catch (err) {
      console.error('Error resolving report:', err);
      alert('Lỗi khi xử lý báo cáo!');
    }
  };

  const handleDismiss = async (reportId: string) => {
    try {
      await updateDoc(doc(db, 'reports', reportId), {
        status: 'dismissed',
        handledBy: adminName,
      });
    } catch (err) {
      console.error('Error dismissing report:', err);
      alert('Lỗi khi bỏ qua báo cáo!');
    }
  };

  const handleRowDoubleClick = async (report: Report) => {
    if (report.targetType !== 'video') return;
    
    setFetchingVideo(true);
    try {
      const videoDoc = await getDoc(doc(db, 'videos', report.targetId));
      if (videoDoc.exists()) {
        const data = videoDoc.data();
        setPlayingVideo({
          videoId: videoDoc.id,
          videoUri: data.videoUri || '',
          authorId: data.authorId || '',
          username: data.username || 'Ẩn danh',
          description: data.description || '',
          timestamp: data.timestamp || 0,
          totalLikes: data.totalLikes || 0,
          totalComments: data.totalComments || 0,
          watchCount: data.watchCount || 0,
          moderationStatus: data.moderationStatus || 'approved',
          aiFlagged: data.aiFlagged || false,
          aiConfidence: data.aiConfidence || 0,
        });
      } else {
        alert('Không tìm thấy video này! Có thể video đã bị xóa.');
      }
    } catch (err) {
      console.error('Error fetching video for report:', err);
      alert('Lỗi khi tải thông tin video.');
    } finally {
      setFetchingVideo(false);
    }
  };

  const filteredReports = useMemo(() => reports.filter((r) => {
    const matchSearch = r.reason.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        r.targetId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                        r.reporterId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchStatus = statusFilter === 'all' || r.status === statusFilter;
    const matchType = typeFilter === 'all' || r.targetType === typeFilter;
    return matchSearch && matchStatus && matchType;
  }), [reports, searchTerm, statusFilter, typeFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredReports.length / PAGE_SIZE));
  const paginatedReports = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredReports.slice(start, start + PAGE_SIZE);
  }, [filteredReports, currentPage, PAGE_SIZE]);

  // Reset page on filter change
  useEffect(() => { setCurrentPage(1); }, [searchTerm, statusFilter, typeFilter]);

  const pendingCount = reports.filter(r => r.status === 'pending').length;
  const resolvedCount = reports.filter(r => r.status === 'resolved').length;
  const dismissedCount = reports.filter(r => r.status === 'dismissed').length;



  const getTargetIcon = (type: string) => {
    switch (type) {
      case 'video': return Video;
      case 'video_ad': return Flag;
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
          <span className="status-badge badge-pending cursor-default">
            ⏳ Chờ: {pendingCount}
          </span>
          <span className="status-badge badge-resolved cursor-default">
            ✅ Đã xử lý: {resolvedCount}
          </span>
          <span className="status-badge badge-dismissed cursor-default">
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
                <option value="video_ad">Quảng cáo</option>
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
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Kháng cáo</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Trạng thái</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/10">
              {loading ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-on-surface-variant">
                    <RefreshCw className="w-5 h-5 animate-spin inline-block mr-2" />
                    Đang tải báo cáo...
                  </td>
                </tr>
              ) : paginatedReports.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-8 text-center text-on-surface-variant">Không tìm thấy báo cáo nào.</td>
                </tr>
              ) : paginatedReports.map((report) => {
                const TargetIcon = getTargetIcon(report.targetType);
                return (
                  <tr 
                    key={report.id} 
                    onDoubleClick={() => handleRowDoubleClick(report)}
                    className={clsx(
                      "hover:bg-surface-high/30 transition-colors group select-none",
                      report.targetType === 'video' && "cursor-pointer"
                    )}
                    title={report.targetType === 'video' ? "Double click để xem video bị tố cáo" : undefined}
                  >
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2">
                        <TargetIcon className="w-4 h-4 text-on-surface-variant" />
                        <span className="font-label text-xs uppercase">
                          {report.targetType === 'video' ? 'Video' : report.targetType === 'video_ad' ? 'Quảng cáo' : report.targetType === 'user' ? 'User' : 'Comment'}
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
                    {report.appeal ? (
                    <div className="bg-primary/10 border border-primary/20 rounded p-2">
                    <p className="text-xs text-primary font-bold mb-1 uppercase">Người dùng phản hồi:</p>
                    <p className="text-sm text-on-surface italic italic">"{report.appeal}"</p>
                    </div>
                    ) : (
                    <span className="text-on-surface-variant/40 text-xs italic">Chưa có phản ánh</span>
                                        )}
                    </td>
                    <td className="py-4 px-6">
                      {report.status === 'pending' && (
                        <span className="status-badge badge-pending">
                          <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse"></span>
                          Chờ xử lý
                        </span>
                      )}
                      {report.status === 'resolved' && (
                        <span className="status-badge badge-resolved">
                          <CheckCircle className="w-3.5 h-3.5" />
                          Đã xử lý
                        </span>
                      )}
                      {report.status === 'dismissed' && (
                        <span className="status-badge badge-dismissed">
                          <XCircle className="w-3.5 h-3.5" />
                          Đã bỏ qua
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6 text-right">
                      {report.status === 'pending' && user?.role !== 'viewer' && (
                        <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button
                            onClick={() => handleResolve(report)}
                            className="p-1.5 text-on-surface-variant hover:text-secondary hover:bg-secondary/10 rounded transition-colors"
                            title="Xử lý và gỡ video"
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
            Hiển thị {((currentPage - 1) * PAGE_SIZE) + 1} đến {Math.min(currentPage * PAGE_SIZE, filteredReports.length)} của {filteredReports.length} báo cáo
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage <= 1}
              className="p-1.5 rounded border border-outline-variant/20 text-on-surface-variant hover:bg-surface-high transition-colors disabled:opacity-50"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            <div className="flex items-center gap-1 px-2">
              {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                <button
                  key={page}
                  onClick={() => setCurrentPage(page)}
                  className={clsx(
                    "w-8 h-8 rounded font-label text-sm font-bold flex items-center justify-center",
                    currentPage === page
                      ? "bg-primary-container/20 text-primary"
                      : "text-on-surface-variant hover:bg-surface-high"
                  )}
                >
                  {page}
                </button>
              ))}
            </div>
            <button
              onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
              disabled={currentPage >= totalPages}
              className="p-1.5 rounded border border-outline-variant/20 text-on-surface-variant hover:bg-surface-high transition-colors disabled:opacity-50"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Playing Video Modal */}
      {playingVideo && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/90 backdrop-blur-md">
          <div className="relative w-full max-w-4xl max-h-screen flex flex-col md:flex-row bg-surface border border-outline-variant/20 rounded-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            {/* Video Player */}
            <div className="relative flex-1 bg-black flex items-center justify-center min-h-[50vh] md:min-h-[80vh]">
              {playingVideo.videoUri ? (
                <video
                  src={playingVideo.videoUri}
                  controls
                  autoPlay
                  className="max-w-full max-h-[80vh] w-auto h-auto object-contain"
                />
              ) : (
                <div className="text-on-surface-variant flex flex-col items-center">
                  <Play className="w-16 h-16 mb-4 opacity-50" />
                  <p>Video không khả dụng</p>
                </div>
              )}
            </div>

            {/* Video Details Side Panel */}
            <div className="w-full md:w-80 flex flex-col bg-surface-low border-l border-outline-variant/10">
              <div className="p-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface">
                <h3 className="font-headline font-bold text-on-surface truncate">Chi tiết Video bị tố cáo</h3>
                <button
                  onClick={() => setPlayingVideo(null)}
                  className="p-1.5 text-on-surface-variant hover:bg-surface-high hover:text-on-surface rounded-full transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-4 flex-1 overflow-y-auto space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold">
                    {playingVideo.username.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="font-label text-sm text-on-surface font-bold">@{playingVideo.username}</p>
                    <p className="text-xs text-on-surface-variant">
                      {new Date(playingVideo.timestamp).toLocaleString('vi-VN')}
                    </p>
                  </div>
                </div>

                <div className="bg-surface-high/50 p-3 rounded-lg border border-outline-variant/10">
                  <p className="font-body text-sm text-on-surface whitespace-pre-wrap">{playingVideo.description || 'Không có mô tả'}</p>
                </div>

                <div className="grid grid-cols-3 gap-2 text-center py-2">
                  <div className="bg-surface p-2 rounded border border-outline-variant/10">
                    <p className="text-xs text-on-surface-variant mb-1">Lượt thích</p>
                    <p className="font-label font-bold text-on-surface">{playingVideo.totalLikes}</p>
                  </div>
                  <div className="bg-surface p-2 rounded border border-outline-variant/10">
                    <p className="text-xs text-on-surface-variant mb-1">Bình luận</p>
                    <p className="font-label font-bold text-on-surface">{playingVideo.totalComments}</p>
                  </div>
                  <div className="bg-surface p-2 rounded border border-outline-variant/10">
                    <p className="text-xs text-on-surface-variant mb-1">Lượt xem</p>
                    <p className="font-label font-bold text-on-surface">{playingVideo.watchCount}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Fetching Video Loading Overlay */}
      {fetchingVideo && (
        <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-background/60 backdrop-blur-sm">
          <Loader2 className="w-10 h-10 animate-spin text-primary mb-2" />
          <span className="text-on-surface font-label text-sm">Đang tải video...</span>
        </div>
      )}
    </div>
  );
}
