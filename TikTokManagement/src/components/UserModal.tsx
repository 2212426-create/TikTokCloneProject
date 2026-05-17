import React from 'react';
import { X, User as UserIcon, Mail, Phone, Calendar, Shield, Activity, Users, Heart } from 'lucide-react';
import { User, UserRole, UserStatus } from '../types';
import { clsx } from 'clsx';

interface UserModalProps {
  user: User | null;
  isOpen: boolean;
  onClose: () => void;
  onUpdateStatus: (userId: string, status: UserStatus) => void;
  onUpdateRole: (userId: string, role: UserRole) => void;
}

export function UserModal({ user, isOpen, onClose, onUpdateStatus, onUpdateRole }: UserModalProps) {
  if (!isOpen || !user) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-surface border border-outline-variant/20 rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto shadow-2xl flex flex-col">
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-center justify-between p-6 border-b border-outline-variant/10 bg-surface/90 backdrop-blur-md">
          <h2 className="text-xl font-headline font-bold text-on-surface">Chi tiết người dùng</h2>
          <button 
            onClick={onClose}
            className="p-2 text-on-surface-variant hover:text-on-surface hover:bg-surface-high rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-8 flex-1">
          {/* Profile Header */}
          <div className="flex items-center gap-6">
            <div className="w-24 h-24 rounded-full bg-surface-high border-2 border-primary/20 overflow-hidden flex-shrink-0">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.username} className="w-full h-full object-cover" />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-primary font-headline text-3xl">
                  {user.username.charAt(1).toUpperCase()}
                </div>
              )}
            </div>
            <div>
              <h3 className="text-2xl font-bold text-on-surface flex items-center gap-2">
                {user.username}
                {user.role === 'admin' && <Shield className="w-5 h-5 text-primary" />}
              </h3>
              <p className="text-on-surface-variant font-label text-sm mt-1">ID: {user.userId}</p>
              <div className="flex items-center gap-3 mt-3">
                <span className={clsx(
                  "px-3 py-1 rounded-full text-xs font-label border",
                  user.status === 'active' ? "bg-secondary-container/10 text-secondary-container border-secondary-container/20" :
                  user.status === 'banned' ? "bg-error/10 text-error border-error/20" :
                  "bg-tertiary-container/10 text-tertiary-container border-tertiary-container/20"
                )}>
                  {user.status === 'active' ? 'Hoạt động' : user.status === 'banned' ? 'Bị khóa' : 'Cảnh cáo'}
                </span>
                <span className="px-3 py-1 rounded-full text-xs font-label border bg-primary/10 text-primary border-primary/20">
                  {user.role === 'admin' ? 'Quản trị viên' : user.role === 'moderator' ? 'Kiểm duyệt viên' : 'Người dùng'}
                </span>
              </div>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-3 gap-4">
            <div className="bg-surface-low p-4 rounded-xl border border-outline-variant/10 text-center">
              <Users className="w-6 h-6 text-primary mx-auto mb-2" />
              <div className="text-2xl font-bold text-on-surface">{user.followers.toLocaleString()}</div>
              <div className="text-xs text-on-surface-variant font-label mt-1">Người theo dõi</div>
            </div>
            <div className="bg-surface-low p-4 rounded-xl border border-outline-variant/10 text-center">
              <UserIcon className="w-6 h-6 text-secondary-container mx-auto mb-2" />
              <div className="text-2xl font-bold text-on-surface">{user.following.toLocaleString()}</div>
              <div className="text-xs text-on-surface-variant font-label mt-1">Đang theo dõi</div>
            </div>
            <div className="bg-surface-low p-4 rounded-xl border border-outline-variant/10 text-center">
              <Heart className="w-6 h-6 text-error mx-auto mb-2" />
              <div className="text-2xl font-bold text-on-surface">{user.likes.toLocaleString()}</div>
              <div className="text-xs text-on-surface-variant font-label mt-1">Lượt thích</div>
            </div>
          </div>

          {/* Detailed Info */}
          <div className="space-y-4">
            <h4 className="text-lg font-headline font-semibold text-on-surface mb-4">Thông tin cá nhân</h4>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-center gap-3 p-3 bg-surface-low rounded-lg border border-outline-variant/10">
                <Mail className="w-5 h-5 text-on-surface-variant" />
                <div>
                  <div className="text-xs text-on-surface-variant font-label">Email</div>
                  <div className="text-sm font-medium text-on-surface">{user.email || 'Chưa cập nhật'}</div>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-surface-low rounded-lg border border-outline-variant/10">
                <Phone className="w-5 h-5 text-on-surface-variant" />
                <div>
                  <div className="text-xs text-on-surface-variant font-label">Số điện thoại</div>
                  <div className="text-sm font-medium text-on-surface">{user.phone || 'Chưa cập nhật'}</div>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-surface-low rounded-lg border border-outline-variant/10">
                <Calendar className="w-5 h-5 text-on-surface-variant" />
                <div>
                  <div className="text-xs text-on-surface-variant font-label">Ngày sinh</div>
                  <div className="text-sm font-medium text-on-surface">{user.birthdate || 'Chưa cập nhật'}</div>
                </div>
              </div>
              <div className="flex items-center gap-3 p-3 bg-surface-low rounded-lg border border-outline-variant/10">
                <Activity className="w-5 h-5 text-on-surface-variant" />
                <div>
                  <div className="text-xs text-on-surface-variant font-label">Ngày tham gia</div>
                  <div className="text-sm font-medium text-on-surface">{new Date(user.createdAt).toLocaleDateString('vi-VN')}</div>
                </div>
              </div>
            </div>
          </div>

          {/* Actions & Settings */}
          <div className="space-y-4 pt-4 border-t border-outline-variant/10">
            <h4 className="text-lg font-headline font-semibold text-on-surface mb-4">Cài đặt hệ thống</h4>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Role Selection */}
              <div className="space-y-2">
                <label className="text-sm font-label text-on-surface-variant">Phân quyền</label>
                <select 
                  value={user.role}
                  onChange={(e) => onUpdateRole(user.userId, e.target.value as UserRole)}
                  className="w-full bg-surface border border-outline-variant/20 text-on-surface font-label text-sm p-3 rounded-lg outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-all"
                >
                  <option value="user">Người dùng (User)</option>
                  <option value="moderator">Kiểm duyệt viên (Moderator)</option>
                  <option value="admin">Quản trị viên (Admin)</option>
                </select>
              </div>

              {/* Status Selection */}
              <div className="space-y-2">
                <label className="text-sm font-label text-on-surface-variant">Trạng thái tài khoản</label>
                <select 
                  value={user.status}
                  onChange={(e) => onUpdateStatus(user.userId, e.target.value as UserStatus)}
                  className={clsx(
                    "w-full border text-on-surface font-label text-sm p-3 rounded-lg outline-none focus:ring-1 transition-all",
                    user.status === 'banned' ? "bg-error/5 border-error/30 focus:border-error focus:ring-error" : 
                    "bg-surface border-outline-variant/20 focus:border-primary focus:ring-primary"
                  )}
                >
                  <option value="active">Hoạt động bình thường</option>
                  <option value="warned">Cảnh cáo</option>
                  <option value="banned">Khóa tài khoản</option>
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
