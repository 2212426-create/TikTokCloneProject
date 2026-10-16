import { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../components/auth-provider';
import {
  Download,
  Search,
  ChevronDown,
  RefreshCw,
  Eye,
  Lock,
  Unlock,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Shield
} from 'lucide-react';
import { clsx } from 'clsx';
import { User, UserRole, UserStatus } from '../types';
import { UserModal } from '../components/UserModal';
import { db, collection, onSnapshot, doc, updateDoc } from '../lib/firebase';

const PAGE_SIZE = 10;

export function Users() {
  const { user: authUser } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);

  // Track selected user ID separately to avoid re-creating the listener
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  useEffect(() => {
    let usersList: User[] = [];
    let profilesMap: Record<string, any> = {};

    const mergeAndSet = () => {
      const merged = usersList.map(u => {
        const profile = profilesMap[u.userId] || {};
        return {
          ...u,
          followers: profile.followers || 0,
          following: profile.following || 0,
          likes: profile.likes || 0,
          avatarUrl: u.avatarUrl || profile.avatarUrl || '',
          username: u.username || profile.username || 'Unknown',
        };
      });
      setUsers(merged);
    };

    // Listen to real-time updates from 'users' collection
    const unsubUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      const uData: User[] = [];
      snapshot.forEach((doc) => {
        const data = doc.data();
        uData.push({
          userId: doc.id,
          username: data.username || 'Unknown',
          avatarUrl: data.avatarUrl || '',
          email: data.email || '',
          phone: data.phone || '',
          birthdate: data.birthdate || '',
          isPrivate: data.isPrivate || false,
          followers: 0,
          following: 0,
          likes: 0,
          role: data.role || 'user',
          status: data.status || 'active',
          createdAt: data.createdAt || Date.now(),
        });
      });
      usersList = uData;
      mergeAndSet();
      setLoading(false);
    }, (error) => {
      console.error("Error fetching users: ", error);
      setLoading(false);
    });

    // Listen to real-time updates from 'profiles' collection
    const unsubProfiles = onSnapshot(collection(db, 'profiles'), (snapshot) => {
      const pMap: Record<string, any> = {};
      snapshot.forEach((doc) => {
        pMap[doc.id] = doc.data();
      });
      profilesMap = pMap;
      mergeAndSet();
    }, (error) => {
      console.error("Error fetching profiles: ", error);
    });

    return () => {
      unsubUsers();
      unsubProfiles();
    };
  }, []); // No dependency on selectedUser — stable listener

  // Update selectedUser when users data changes
  useEffect(() => {
    if (selectedUserId) {
      const updated = users.find(u => u.userId === selectedUserId);
      if (updated) {
        setSelectedUser(updated);
      }
    }
  }, [users, selectedUserId]);

  const handleUpdateStatus = async (userId: string, newStatus: UserStatus) => {
    // Confirm before ban
    if (newStatus === 'banned') {
      const confirmed = window.confirm('Bạn có chắc chắn muốn khóa tài khoản này? Người dùng sẽ không thể truy cập ứng dụng.');
      if (!confirmed) return;
    }
    try {
      const userRef = doc(db, 'users', userId);
      await updateDoc(userRef, {
        status: newStatus
      });
    } catch (error) {
      console.error("Error updating status: ", error);
      alert("Lỗi khi cập nhật trạng thái!");
    }
  };

  const handleUpdateRole = async (userId: string, newRole: UserRole) => {
    try {
      const userRef = doc(db, 'users', userId);
      await updateDoc(userRef, {
        role: newRole
      });
    } catch (error) {
      console.error("Error updating role: ", error);
      alert("Lỗi khi phân quyền!");
    }
  };

  const filteredUsers = useMemo(() => {
    return users.filter(user => {
      const matchSearch = user.username.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          user.userId.toLowerCase().includes(searchTerm.toLowerCase());
      const matchStatus = statusFilter === 'all' || user.status === statusFilter;
      return matchSearch && matchStatus;
    });
  }, [users, searchTerm, statusFilter]);

  // Pagination logic
  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
  const paginatedUsers = useMemo(() => {
    const start = (currentPage - 1) * PAGE_SIZE;
    return filteredUsers.slice(start, start + PAGE_SIZE);
  }, [filteredUsers, currentPage]);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter]);

  const handleExportCSV = () => {
    if (filteredUsers.length === 0) {
      alert('Không có dữ liệu để xuất!');
      return;
    }

    const headers = ['User ID', 'Tên người dùng', 'Email', 'Số điện thoại', 'Vai trò', 'Trạng thái', 'Followers', 'Following', 'Likes', 'Ngày tạo'];

    const roleLabel = (role: string) => {
      switch (role) {
        case 'admin': return 'Admin';
        case 'moderator': return 'Moderator';
        case 'viewer': return 'Viewer';
        default: return 'User';
      }
    };
    const statusLabel = (status: string) => {
      switch (status) {
        case 'active': return 'Hoạt động';
        case 'banned': return 'Bị khóa';
        case 'warned': return 'Cảnh cáo';
        default: return status;
      }
    };

    const escapeCSV = (val: string) => {
      if (val.includes(',') || val.includes('"') || val.includes('\n')) {
        return `"${val.replace(/"/g, '""')}"`;
      }
      return val;
    };

    const rows = filteredUsers.map(u => [
      u.userId,
      u.username,
      u.email,
      u.phone || '',
      roleLabel(u.role),
      statusLabel(u.status),
      u.followers.toString(),
      u.following.toString(),
      u.likes.toString(),
      new Date(u.createdAt).toLocaleDateString('vi-VN'),
    ].map(escapeCSV).join(','));

    const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `toptop_users_${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const openUserModal = (user: User) => {
    setSelectedUserId(user.userId);
    setSelectedUser(user);
  };

  const closeUserModal = () => {
    setSelectedUserId(null);
    setSelectedUser(null);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-headline text-3xl font-bold text-on-surface mb-2">Quản lý người dùng</h2>
          <p className="text-on-surface-variant font-body text-base">Giám sát và quản lý trạng thái tài khoản trên hệ thống TopTop.</p>
        </div>
        <div className="flex items-center gap-3">
          <button 
            onClick={handleExportCSV}
            className="flex items-center gap-2 px-4 py-2 border border-outline-variant rounded-lg font-label text-sm text-on-surface hover:bg-surface-high transition-colors"
          >
            <Download className="w-4 h-4" />
            Xuất CSV
          </button>
        </div>
      </div>

      {/* Filters & DataTable Card */}
      <div className="bg-surface-low border border-outline-variant/20 rounded-xl overflow-hidden relative shadow-sm">
        <div className="absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-primary/50 to-transparent"></div>
        
        {/* Toolbar */}
        <div className="p-6 border-b border-outline-variant/10 flex flex-col sm:flex-row gap-4 justify-between items-center bg-surface/30 backdrop-blur-md">
          <div className="relative w-full sm:max-w-xs group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-on-surface-variant group-focus-within:text-primary transition-colors" />
            <input
              type="text"
              placeholder="Tìm theo Tên hoặc ID..."
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
                <option value="active">Hoạt động</option>
                <option value="banned">Bị khóa</option>
                <option value="warned">Cảnh cáo</option>
              </select>
              <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant pointer-events-none" />
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead>
              <tr className="border-b border-outline-variant/10 bg-surface/50">
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold w-16">Avatar</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold min-w-[200px]">Tên người dùng</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Vai trò</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Tương tác</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold">Trạng thái</th>
                <th className="py-3 px-6 font-label text-xs text-on-surface-variant uppercase tracking-wider font-semibold text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/10">
              {loading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-on-surface-variant">
                    <RefreshCw className="w-5 h-5 animate-spin inline-block mr-2" />
                    Đang tải dữ liệu...
                  </td>
                </tr>
              ) : paginatedUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-on-surface-variant">Không tìm thấy người dùng nào.</td>
                </tr>
              ) : paginatedUsers.map((user) => (
                <tr key={user.userId} className="hover:bg-surface-high/30 transition-colors group">
                  <td className="py-4 px-6">
                    <div className={clsx("w-10 h-10 rounded-full bg-surface-high border border-outline-variant/20 overflow-hidden flex items-center justify-center font-bold text-primary", user.status === 'banned' && "grayscale opacity-70")}>
                      {user.avatarUrl ? (
                        <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
                      ) : (
                        user.username.charAt(0).toUpperCase()
                      )}
                    </div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="font-medium text-on-surface flex items-center gap-1">
                      {user.username}
                      {user.role === 'admin' && <Shield className="w-4 h-4 text-primary" />}
                    </div>
                    <div className="font-label text-xs text-on-surface-variant mt-0.5 font-mono">{user.userId}</div>
                  </td>
                  <td className="py-4 px-6">
                    <span className={clsx(
                      "font-label text-xs px-2.5 py-1 rounded border font-semibold shadow-sm",
                      user.role === 'admin' ? "bg-primary/10 text-primary border-primary/20 dark:bg-primary-container/30 dark:text-primary-fixed dark:border-primary/30" : 
                      user.role === 'moderator' ? "bg-secondary/10 text-secondary border-secondary/20 dark:bg-secondary-container/30 dark:text-secondary-fixed dark:border-secondary/30" : 
                      user.role === 'viewer' ? "bg-tertiary/10 text-tertiary border-tertiary/20 dark:bg-tertiary-container/30 dark:text-tertiary-fixed dark:border-tertiary/30" :
                      "bg-surface-variant/40 text-on-surface-variant border-outline-variant/20 dark:bg-surface-variant/10 dark:text-on-surface-variant dark:border-outline-variant/20"
                    )}>
                      {user.role === 'admin' ? 'Admin' : user.role === 'moderator' ? 'Moderator' : user.role === 'viewer' ? 'Viewer' : 'User'}
                    </span>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm text-on-surface">{user.followers.toLocaleString()} fl</div>
                    <div className="text-xs text-on-surface-variant">{user.likes.toLocaleString()} likes</div>
                  </td>
                  <td className="py-4 px-6">
                    {user.status === 'active' && (
                      <span className="status-badge badge-resolved">
                        <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                        Hoạt động
                      </span>
                    )}
                    {user.status === 'banned' && (
                      <span className="status-badge badge-rejected">
                        <Lock className="w-3.5 h-3.5" />
                        Bị khóa
                      </span>
                    )}
                    {user.status === 'warned' && (
                      <span className="status-badge badge-pending">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        Cảnh cáo
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => openUserModal(user)}
                        className="p-1.5 text-on-surface-variant hover:text-secondary hover:bg-secondary/10 rounded transition-colors" 
                        title="Xem chi tiết"
                      >
                        <Eye className="w-5 h-5" />
                      </button>
                      
                      {authUser?.role !== 'viewer' && (
                        <>
                          {user.status === 'banned' ? (
                            <button 
                              onClick={() => handleUpdateStatus(user.userId, 'active')}
                              className="p-1.5 text-on-surface-variant hover:text-secondary hover:bg-secondary/10 rounded transition-colors" 
                              title="Mở khóa"
                            >
                              <Unlock className="w-5 h-5" />
                            </button>
                          ) : (
                            <button 
                              onClick={() => handleUpdateStatus(user.userId, 'banned')}
                              className="p-1.5 text-on-surface-variant hover:text-error hover:bg-error/10 rounded transition-colors" 
                              title="Khóa tài khoản"
                            >
                              <Lock className="w-5 h-5" />
                            </button>
                          )}
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-6 border-t border-outline-variant/10 flex items-center justify-between bg-surface-lowest/50">
          <p className="font-label text-sm text-on-surface-variant">
            Hiển thị {((currentPage - 1) * PAGE_SIZE) + 1} đến {Math.min(currentPage * PAGE_SIZE, filteredUsers.length)} của {filteredUsers.length} người dùng
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
              {Array.from({ length: totalPages }, (_, i) => i + 1)
                .filter(page => page === 1 || page === totalPages || Math.abs(page - currentPage) <= 1)
                .reduce<(number | 'ellipsis')[]>((acc, page, idx, arr) => {
                  if (idx > 0 && page - (arr[idx - 1] as number) > 1) acc.push('ellipsis');
                  acc.push(page);
                  return acc;
                }, [])
                .map((item, idx) =>
                  item === 'ellipsis' ? (
                    <span key={`ellipsis-${idx}`} className="px-1 text-on-surface-variant">…</span>
                  ) : (
                    <button
                      key={item}
                      onClick={() => setCurrentPage(item)}
                      className={clsx(
                        "w-8 h-8 rounded font-label text-sm font-bold flex items-center justify-center",
                        currentPage === item
                          ? "bg-primary-container/20 text-primary"
                          : "text-on-surface-variant hover:bg-surface-high"
                      )}
                    >
                      {item}
                    </button>
                  )
                )}
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

      <UserModal 
        user={selectedUser} 
        isOpen={!!selectedUser} 
        onClose={closeUserModal}
        onUpdateStatus={handleUpdateStatus}
        onUpdateRole={handleUpdateRole}
      />

    </div>
  );
}
