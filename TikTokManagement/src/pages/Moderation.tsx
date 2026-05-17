import { useState, useEffect } from 'react';
import { Filter, Play, AlertTriangle, EyeOff, Flag, Trash2, Check, RefreshCw, X } from 'lucide-react';
import { clsx } from 'clsx';
import { db, collection, onSnapshot, doc, updateDoc } from '../lib/firebase';
import type { Video } from '../types';

type TabFilter = 'pending' | 'approved' | 'rejected';

export function Moderation() {
  const [videos, setVideos] = useState<Video[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<TabFilter>('pending');
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  useEffect(() => {
    const unsubscribe = onSnapshot(collection(db, 'videos'), (snapshot) => {
      const videosData: Video[] = [];
      snapshot.forEach((docSnap) => {
        const data = docSnap.data();
        videosData.push({
          videoId: docSnap.id,
          videoUri: data.videoUri || '',
          authorId: data.authorId || '',
          username: data.username || 'Ẩn danh',
          description: data.description || '',
          timestamp: data.timestamp || 0,
          totalLikes: data.totalLikes || 0,
          totalComments: data.totalComments || 0,
          watchCount: data.watchCount || 0,
          moderationStatus: data.moderationStatus || 'pending',
          aiFlagged: data.aiFlagged || false,
          aiConfidence: data.aiConfidence || 0,
          rejectedReason: data.rejectedReason || '',
          reviewedBy: data.reviewedBy || '',
        });
      });
      // Sort newest first
      videosData.sort((a, b) => b.timestamp - a.timestamp);
      setVideos(videosData);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleApprove = async (videoId: string) => {
    try {
      await updateDoc(doc(db, 'videos', videoId), {
        moderationStatus: 'approved',
        reviewedBy: 'admin_web',
      });
    } catch (err) {
      console.error('Error approving video:', err);
      alert('Lỗi khi duyệt video!');
    }
  };

  const handleReject = async (videoId: string) => {
    try {
      await updateDoc(doc(db, 'videos', videoId), {
        moderationStatus: 'rejected',
        rejectedReason: rejectReason || 'Vi phạm tiêu chuẩn cộng đồng',
        reviewedBy: 'admin_web',
      });
      setRejectingId(null);
      setRejectReason('');
    } catch (err) {
      console.error('Error rejecting video:', err);
      alert('Lỗi khi từ chối video!');
    }
  };

  const filteredVideos = videos.filter((v) => v.moderationStatus === activeTab);
  const pendingCount = videos.filter((v) => v.moderationStatus === 'pending').length;
  const approvedCount = videos.filter((v) => v.moderationStatus === 'approved').length;
  const rejectedCount = videos.filter((v) => v.moderationStatus === 'rejected').length;

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

  return (
    <div className="space-y-6">
      {/* Page Header & Tabs */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-headline text-3xl font-bold text-on-surface mb-2">Kiểm duyệt Video</h2>
          <p className="font-body text-base text-on-surface-variant max-w-2xl">
            Quản lý và duyệt các video được tải lên từ App. Dữ liệu đồng bộ real-time với Firestore.
          </p>
        </div>
        <div className="flex bg-surface-low rounded-lg p-1 border border-outline-variant/20 inline-flex">
          <button 
            onClick={() => setActiveTab('pending')}
            className={clsx("px-4 py-2 rounded-md font-label text-sm transition-all", activeTab === 'pending' ? "bg-surface-highest text-on-surface shadow-sm font-bold" : "text-on-surface-variant hover:text-on-surface hover:bg-surface-high")}
          >
            Chờ duyệt ({pendingCount})
          </button>
          <button 
            onClick={() => setActiveTab('approved')}
            className={clsx("px-4 py-2 rounded-md font-label text-sm transition-all", activeTab === 'approved' ? "bg-surface-highest text-on-surface shadow-sm font-bold" : "text-on-surface-variant hover:text-on-surface hover:bg-surface-high")}
          >
            Đã duyệt ({approvedCount})
          </button>
          <button 
            onClick={() => setActiveTab('rejected')}
            className={clsx("px-4 py-2 rounded-md font-label text-sm transition-all", activeTab === 'rejected' ? "bg-surface-highest text-on-surface shadow-sm font-bold" : "text-on-surface-variant hover:text-on-surface hover:bg-surface-high")}
          >
            Đã gỡ bỏ ({rejectedCount})
          </button>
        </div>
      </div>

      {/* Loading State */}
      {loading ? (
        <div className="flex items-center justify-center py-20">
          <RefreshCw className="w-8 h-8 animate-spin text-primary" />
          <span className="ml-3 text-on-surface-variant font-label">Đang tải video từ Firestore...</span>
        </div>
      ) : filteredVideos.length === 0 ? (
        <div className="text-center py-20 text-on-surface-variant">
          <p className="text-lg font-headline mb-2">Không có video nào</p>
          <p className="font-label text-sm">
            {activeTab === 'pending' ? 'Tất cả video đã được xử lý.' : 
             activeTab === 'approved' ? 'Chưa có video nào được duyệt.' : 
             'Chưa có video nào bị từ chối.'}
          </p>
        </div>
      ) : (
        /* Grid of Videos */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredVideos.map((video) => (
            <div key={video.videoId} className={clsx(
              "glass-panel glass-panel-hover rounded-xl overflow-hidden flex flex-col transition-all duration-300",
              video.aiFlagged && "border-error/30"
            )}>
              {/* Thumbnail / Video Preview */}
              <div className="relative w-full aspect-[9/16] bg-surface-highest group cursor-pointer overflow-hidden">
                {video.videoUri ? (
                  <video 
                    src={video.videoUri} 
                    className="w-full h-full object-cover opacity-80 group-hover:opacity-100 transition-all duration-300"
                    muted
                    preload="metadata"
                    onMouseEnter={(e) => (e.target as HTMLVideoElement).play()}
                    onMouseLeave={(e) => { const v = e.target as HTMLVideoElement; v.pause(); v.currentTime = 0; }}
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center bg-surface-high">
                    <Play className="w-12 h-12 text-on-surface-variant/30" />
                  </div>
                )}
                
                {/* Play Overlay */}
                <div className="absolute inset-0 flex items-center justify-center bg-background/20 opacity-0 group-hover:opacity-100 transition-opacity">
                  <div className="w-12 h-12 rounded-full bg-background/60 backdrop-blur flex items-center justify-center border border-on-surface/20">
                    <Play className="w-6 h-6 text-on-surface fill-current" />
                  </div>
                </div>

                {/* Badges */}
                <div className="absolute top-3 left-3 right-3 flex justify-between items-start">
                  {video.aiFlagged ? (
                    <div className="backdrop-blur px-2 py-0.5 rounded flex items-center gap-1 font-label text-xs border bg-error-container/90 text-on-error-container border-error/20">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      AI: {video.aiConfidence}%
                    </div>
                  ) : (
                    <div className="backdrop-blur px-2 py-0.5 rounded flex items-center gap-1 font-label text-xs border bg-surface-variant/90 text-on-surface-variant border-outline/20">
                      <Flag className="w-3.5 h-3.5" />
                      Chờ duyệt
                    </div>
                  )}
                  <div className="bg-surface/80 backdrop-blur text-on-surface px-2 py-0.5 rounded font-label text-[10px]">
                    ❤ {video.totalLikes} · 👁 {video.watchCount}
                  </div>
                </div>

                {/* Rejection badge */}
                {video.moderationStatus === 'rejected' && (
                  <div className="absolute bottom-3 left-3 right-3">
                    <div className="bg-error/90 backdrop-blur text-on-error px-3 py-1.5 rounded font-label text-xs">
                      Lý do: {video.rejectedReason || 'Không rõ'}
                    </div>
                  </div>
                )}
              </div>

              {/* Content */}
              <div className="p-4 flex-1 flex flex-col bg-surface-low/50">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 rounded-full bg-primary/20 flex items-center justify-center text-primary font-bold text-sm shrink-0">
                    {video.username.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="font-label text-sm text-on-surface truncate">@{video.username}</p>
                    <p className="text-[10px] text-on-surface-variant truncate">{formatTimeAgo(video.timestamp)}</p>
                  </div>
                </div>
                <p className="font-body text-sm text-on-surface-variant line-clamp-2 mb-4 flex-1">
                  {video.description || 'Không có mô tả'}
                </p>

                {/* Actions */}
                {activeTab === 'pending' && (
                  <div className="grid grid-cols-2 gap-2 mt-auto">
                    {rejectingId === video.videoId ? (
                      <div className="col-span-2 space-y-2">
                        <input 
                          type="text"
                          placeholder="Lý do từ chối..."
                          value={rejectReason}
                          onChange={(e) => setRejectReason(e.target.value)}
                          className="w-full bg-surface border border-outline-variant/20 text-on-surface font-label text-sm p-2 rounded outline-none focus:border-error"
                        />
                        <div className="grid grid-cols-2 gap-2">
                          <button 
                            onClick={() => handleReject(video.videoId)}
                            className="py-2 rounded bg-error text-on-error hover:bg-error/90 font-label text-sm font-bold transition-colors"
                          >
                            Xác nhận gỡ
                          </button>
                          <button 
                            onClick={() => { setRejectingId(null); setRejectReason(''); }}
                            className="py-2 rounded border border-outline-variant text-on-surface hover:bg-surface-high font-label text-sm transition-colors"
                          >
                            Hủy
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <button 
                          onClick={() => setRejectingId(video.videoId)}
                          className="py-2 rounded border border-error text-error hover:bg-error/10 font-label text-sm flex items-center justify-center gap-1.5 transition-colors"
                        >
                          <Trash2 className="w-4 h-4" /> Gỡ bỏ
                        </button>
                        <button 
                          onClick={() => handleApprove(video.videoId)}
                          className="py-2 rounded bg-secondary-container/20 text-secondary-container border border-secondary-container/30 hover:bg-secondary-container/30 font-label text-sm flex items-center justify-center gap-1.5 transition-colors"
                        >
                          <Check className="w-4 h-4" /> Duyệt
                        </button>
                      </>
                    )}
                  </div>
                )}

                {activeTab === 'approved' && (
                  <div className="mt-auto">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-secondary-container/10 text-secondary-container font-label text-xs border border-secondary-container/20">
                      <Check className="w-3.5 h-3.5" />
                      Đã được duyệt
                    </span>
                  </div>
                )}

                {activeTab === 'rejected' && (
                  <div className="mt-auto flex items-center gap-2">
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-error/10 text-error font-label text-xs border border-error/20">
                      <X className="w-3.5 h-3.5" />
                      Đã gỡ bỏ
                    </span>
                    <button 
                      onClick={() => handleApprove(video.videoId)}
                      className="text-xs font-label text-primary hover:underline"
                    >
                      Khôi phục
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
