import { useState, useEffect } from 'react';
import { collection, query, onSnapshot, doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Store, CheckCircle, XCircle, AlertCircle, Eye, Search } from 'lucide-react';

interface StoreRegistrationItem {
  id: string;
  storeName: string;
  ownerName: string;
  storeType: string;
  businessSubtype?: string;
  contactName: string;
  phone: string;
  region: string;
  address: string;
  status: string;
  operatingStatus: string;
  rejectionReason?: string;
  verificationDocs: string[];
  taxCode?: string;
  representative?: string;
  updatedAt: number;
}

export function StoreModeration() {
  const [stores, setStores] = useState<StoreRegistrationItem[]>([]);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'pending' | 'needs_changes' | 'approved' | 'rejected'>('pending');
  const [selectedDocs, setSelectedDocs] = useState<string[] | null>(null);

  useEffect(() => {
    const q = query(collection(db, 'stores'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const items: StoreRegistrationItem[] = snapshot.docs.map((docSnap) => {
        const data = docSnap.data();
        return {
          id: docSnap.id,
          storeName: data.storeName || '',
          ownerName: data.ownerName || '',
          storeType: data.storeType || 'individual',
          businessSubtype: data.businessSubtype,
          contactName: data.contactName || '',
          phone: data.phone || '',
          region: data.region || '',
          address: data.address || '',
          status: data.status || 'pending',
          operatingStatus: data.operatingStatus || 'active',
          rejectionReason: data.rejectionReason,
          verificationDocs: data.verificationDocs || [],
          taxCode: data.taxCode,
          representative: data.representative,
          updatedAt: data.updatedAt || 0,
        };
      });
      setStores(items);
    });

    return () => unsubscribe();
  }, []);

  const handleApprove = async (id: string) => {
    await updateDoc(doc(db, 'stores', id), {
      status: 'approved',
      operatingStatus: 'active',
      rejectionReason: null,
      updatedAt: Date.now(),
    });
  };

  const handleRequestChanges = async (id: string) => {
    const reason = prompt('Nhập nội dung cần bổ sung / chỉnh sửa:');
    if (reason) {
      await updateDoc(doc(db, 'stores', id), {
        status: 'needs_changes',
        rejectionReason: reason,
        updatedAt: Date.now(),
      });
    }
  };

  const handleReject = async (id: string) => {
    const reason = prompt('Nhập lý do từ chối đăng ký gian hàng:');
    if (reason) {
      await updateDoc(doc(db, 'stores', id), {
        status: 'rejected',
        rejectionReason: reason,
        updatedAt: Date.now(),
      });
    }
  };

  const handleToggleOperatingStatus = async (id: string, currentStatus: string) => {
    const nextStatus = currentStatus === 'active' ? 'suspended' : 'active';
    await updateDoc(doc(db, 'stores', id), {
      operatingStatus: nextStatus,
      updatedAt: Date.now(),
    });
  };

  const filteredStores = stores.filter((s) => {
    const matchesSearch = s.storeName.toLowerCase().includes(search.toLowerCase()) || s.contactName.toLowerCase().includes(search.toLowerCase()) || s.phone.includes(search);
    const matchesFilter = filter === 'all' || s.status === filter;
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-on-surface flex items-center gap-2">
            <Store className="w-7 h-7 text-primary" />
            Kiểm Duyệt Đăng Ký Gian Hàng Nông Nghiệp
          </h2>
          <p className="text-sm text-on-surface-variant">Thẩm định hồ sơ nhà vườn, đại lý vật tư, doanh nghiệp nông nghiệp</p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex gap-4 items-center bg-surface-low p-4 rounded-xl">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3 top-3 text-on-surface-variant" />
          <input
            type="text"
            placeholder="Tìm theo tên gian hàng, SĐT, người liên hệ..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-surface border border-outline-variant/30 rounded-lg text-sm"
          />
        </div>

        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value as any)}
          className="px-3 py-2 bg-surface border border-outline-variant/30 rounded-lg text-sm"
        >
          <option value="pending">Chờ xét duyệt</option>
          <option value="needs_changes">Cần bổ sung</option>
          <option value="approved">Đã duyệt</option>
          <option value="rejected">Bị từ chối</option>
          <option value="all">Tất cả hồ sơ</option>
        </select>
      </div>

      {/* Store Registrations Table */}
      <div className="bg-surface-low rounded-xl overflow-hidden border border-outline-variant/20">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-outline-variant/20 text-xs font-semibold text-on-surface-variant uppercase bg-surface">
              <th className="p-4">Gian hàng</th>
              <th className="p-4">Loại hình</th>
              <th className="p-4">Người liên hệ / SĐT</th>
              <th className="p-4">Khu vực / Địa chỉ</th>
              <th className="p-4">Giấy tờ xác minh</th>
              <th className="p-4">Trạng thái hồ sơ</th>
              <th className="p-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/10 text-sm">
            {filteredStores.map((store) => (
              <tr key={store.id} className="hover:bg-surface-high/50">
                <td className="p-4 font-semibold text-on-surface">{store.storeName}</td>
                <td className="p-4">
                  <span className="px-2 py-0.5 text-xs rounded bg-surface-high text-on-surface">
                    {store.storeType === 'individual' ? 'Cá nhân / Nhà vườn' : `Đơn vị (${store.businessSubtype || 'Doanh nghiệp'})`}
                  </span>
                </td>
                <td className="p-4">
                  <div>{store.contactName}</div>
                  <div className="text-xs text-on-surface-variant">{store.phone}</div>
                </td>
                <td className="p-4">
                  <div>{store.region}</div>
                  <div className="text-xs text-on-surface-variant">{store.address}</div>
                </td>
                <td className="p-4">
                  {store.verificationDocs.length > 0 ? (
                    <button
                      onClick={() => setSelectedDocs(store.verificationDocs)}
                      className="flex items-center gap-1 text-xs text-primary hover:underline font-semibold"
                    >
                      <Eye className="w-3.5 h-3.5" /> Xem ({store.verificationDocs.length})
                    </button>
                  ) : (
                    <span className="text-xs text-on-surface-variant">Không có</span>
                  )}
                </td>
                <td className="p-4">
                  <div className="flex flex-col gap-1">
                    <span
                      className={`px-2 py-0.5 text-xs rounded-full font-semibold w-max ${
                        store.status === 'approved'
                          ? 'bg-green-500/20 text-green-400'
                          : store.status === 'rejected'
                          ? 'bg-red-500/20 text-red-400'
                          : store.status === 'needs_changes'
                          ? 'bg-orange-500/20 text-orange-400'
                          : 'bg-yellow-500/20 text-yellow-400'
                      }`}
                    >
                      {store.status === 'approved'
                        ? 'Đã duyệt'
                        : store.status === 'rejected'
                        ? 'Bị từ chối'
                        : store.status === 'needs_changes'
                        ? 'Cần bổ sung'
                        : 'Chờ duyệt'}
                    </span>
                    {store.rejectionReason && (
                      <span className="text-xs text-red-400 italic">Lý do: {store.rejectionReason}</span>
                    )}
                  </div>
                </td>
                <td className="p-4 text-right space-x-2">
                  <button
                    onClick={() => handleApprove(store.id)}
                    className="p-1.5 hover:bg-green-500/20 text-green-400 rounded-lg"
                    title="Phê duyệt gian hàng"
                  >
                    <CheckCircle className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleRequestChanges(store.id)}
                    className="p-1.5 hover:bg-orange-500/20 text-orange-400 rounded-lg"
                    title="Yêu cầu bổ sung thông tin"
                  >
                    <AlertCircle className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleReject(store.id)}
                    className="p-1.5 hover:bg-red-500/20 text-red-400 rounded-lg"
                    title="Từ chối hồ sơ"
                  >
                    <XCircle className="w-5 h-5" />
                  </button>
                  {store.status === 'approved' && (
                    <button
                      onClick={() => handleToggleOperatingStatus(store.id, store.operatingStatus)}
                      className={`px-2 py-1 text-xs rounded ${store.operatingStatus === 'active' ? 'bg-red-500/20 text-red-400' : 'bg-green-500/20 text-green-400'}`}
                    >
                      {store.operatingStatus === 'active' ? 'Tạm ngưng' : 'Mở lại'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Verification Docs Preview Modal */}
      {selectedDocs && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
          <div className="bg-surface p-6 rounded-2xl max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-lg text-on-surface">Tài Liệu Xác Minh Gian Hàng (Bảo mật)</h3>
              <button onClick={() => setSelectedDocs(null)} className="text-on-surface-variant hover:text-on-surface font-bold">✕</button>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {selectedDocs.map((url, i) => (
                <div key={i} className="border border-outline-variant/30 rounded-lg overflow-hidden bg-black">
                  <img src={url} alt={`Doc ${i}`} className="w-full h-48 object-contain" />
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
