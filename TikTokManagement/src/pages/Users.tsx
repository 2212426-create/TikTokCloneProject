import { useState, useEffect } from 'react';
import {
  Download,
  UserPlus,
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

export function Users() {
  const [users, setUsers] = useState<User[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Listen to real-time updates from 'users' collection
    const unsubscribe = onSnapshot(collection(db, 'users'), (snapshot) => {
      const usersData: User[] = [];
      snapshot.forEach((doc) => {
        const data = doc.data();
        usersData.push({
          userId: doc.id,
          username: data.username || 'Unknown',
          avatarUrl: data.avatarUrl || '',
          email: data.email || '',
          phone: data.phone || '',
          birthdate: data.birthdate || '',
          isPrivate: data.isPrivate || false,
          followers: data.followers || 0,
          following: data.following || 0,
          likes: data.likes || 0,
          role: data.role || 'user',
          status: data.status || 'active',
          createdAt: data.createdAt || Date.now(),
        });
      });
      setUsers(usersData);
      
      // Update selected user if currently open
      if (selectedUser) {
        const updatedSelected = usersData.find(u => u.userId === selectedUser.userId);
        if (updatedSelected) {
          setSelectedUser(updatedSelected);
        }
      }
      setLoading(false);
    }, (error) => {
      console.error("Error fetching users: ", error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [selectedUser?.userId]);

  const handleUpdateStatus = async (userId: string, newStatus: UserStatus) => {
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

  const filteredUsers = users.filter(user => {
    const matchSearch = user.username.toLowerCase().includes(searchTerm.toLowerCase()) || 
                        user.userId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchStatus = statusFilter === 'all' || user.status === statusFilter;
    return matchSearch && matchStatus;
  });

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <h2 className="font-headline text-3xl font-bold text-on-surface mb-2">Quản lý người dùng</h2>
          <p className="text-on-surface-variant font-body text-base">Giám sát và quản lý trạng thái tài khoản trên hệ thống TopTop.</p>
        </div>
        <div className="flex items-center gap-3">
          <button className="flex items-center gap-2 px-4 py-2 border border-outline-variant rounded-lg font-label text-sm text-on-surface hover:bg-surface-high transition-colors">
            <Download className="w-4 h-4" />
            Xuất CSV
          </button>
          <button className="flex items-center gap-2 px-4 py-2 bg-primary text-on-primary rounded-lg font-label text-sm hover:bg-primary/90 transition-colors shadow-lg shadow-primary/30">
            <UserPlus className="w-4 h-4" />
            Thêm người dùng
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
            <button className="p-2 border border-outline-variant/20 rounded-lg text-on-surface-variant hover:text-on-surface hover:bg-surface transition-colors">
              <RefreshCw className={clsx("w-5 h-5", loading && "animate-spin")} />
            </button>
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
                  <td colSpan={6} className="py-8 text-center text-on-surface-variant">Đang tải dữ liệu...</td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-on-surface-variant">Không tìm thấy người dùng nào.</td>
                </tr>
              ) : filteredUsers.map((user) => (
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
                      "font-label text-xs px-2 py-1 rounded",
                      user.role === 'admin' ? "bg-primary/10 text-primary" : 
                      user.role === 'moderator' ? "bg-secondary-container/10 text-secondary-container" : 
                      "text-on-surface-variant"
                    )}>
                      {user.role === 'admin' ? 'Admin' : user.role === 'moderator' ? 'Moderator' : 'User'}
                    </span>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm text-on-surface">{user.followers.toLocaleString()} fl</div>
                    <div className="text-xs text-on-surface-variant">{user.likes.toLocaleString()} likes</div>
                  </td>
                  <td className="py-4 px-6">
                    {user.status === 'active' && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-secondary-container/10 text-secondary-container font-label text-xs border border-secondary-container/20">
                        <span className="w-1.5 h-1.5 rounded-full bg-secondary-container"></span>
                        Hoạt động
                      </span>
                    )}
                    {user.status === 'banned' && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-error/10 text-error font-label text-xs border border-error/20">
                        <Lock className="w-3.5 h-3.5" />
                        Bị khóa
                      </span>
                    )}
                    {user.status === 'warned' && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-tertiary/10 text-tertiary-container font-label text-xs border border-tertiary/30">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        Cảnh cáo
                      </span>
                    )}
                  </td>
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => setSelectedUser(user)}
                        className="p-1.5 text-on-surface-variant hover:text-secondary-container hover:bg-secondary-container/10 rounded transition-colors" 
                        title="Xem chi tiết"
                      >
                        <Eye className="w-5 h-5" />
                      </button>
                      {user.status === 'banned' ? (
                        <button 
                          onClick={() => handleUpdateStatus(user.userId, 'active')}
                          className="p-1.5 text-on-surface-variant hover:text-secondary-container hover:bg-secondary-container/10 rounded transition-colors" 
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
            Hiển thị 1 đến {filteredUsers.length} của {filteredUsers.length} người dùng
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

      <UserModal 
        user={selectedUser} 
        isOpen={!!selectedUser} 
        onClose={() => setSelectedUser(null)} 
        onUpdateStatus={handleUpdateStatus}
        onUpdateRole={handleUpdateRole}
      />
    </div>
  );
}

