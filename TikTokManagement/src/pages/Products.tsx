import { useState, useEffect } from 'react';
import { collection, query, onSnapshot, doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { ShoppingBag, CheckCircle, XCircle, EyeOff, Search } from 'lucide-react';

interface ProductItem {
  id: string;
  name: string;
  sellerName: string;
  category: string;
  saleType: string;
  listedPrice: number;
  referencePrice: number;
  region: string;
  status: string;
  moderationStatus: string;
  rejectionReason?: string;
}

export function Products() {
  const [products, setProducts] = useState<ProductItem[]>([]);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'pending' | 'approved' | 'rejected'>('all');

  useEffect(() => {
    const q = query(collection(db, 'products'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const items: ProductItem[] = snapshot.docs.map((docSnap) => {
        const data = docSnap.data();
        return {
          id: docSnap.id,
          name: data.name || '',
          sellerName: data.sellerName || 'Nông hộ',
          category: data.category || 'Nông sản',
          saleType: data.saleType || 'fixed_price',
          listedPrice: data.listedPrice || 0,
          referencePrice: data.referencePrice || 0,
          region: data.region || 'Chưa rõ',
          status: data.status || 'active',
          moderationStatus: data.moderationStatus || 'approved',
          rejectionReason: data.rejectionReason,
        };
      });
      setProducts(items);
    });

    return () => unsubscribe();
  }, []);

  const handleApprove = async (id: string) => {
    await updateDoc(doc(db, 'products', id), {
      moderationStatus: 'approved',
      status: 'active',
    });
  };

  const handleReject = async (id: string) => {
    const reason = prompt('Nhập lý do từ chối / ẩn sản phẩm:');
    if (reason) {
      await updateDoc(doc(db, 'products', id), {
        moderationStatus: 'rejected',
        status: 'hidden',
        rejectionReason: reason,
      });
    }
  };

  const handleToggleHide = async (id: string, currentStatus: string) => {
    const nextStatus = currentStatus === 'active' ? 'hidden' : 'active';
    await updateDoc(doc(db, 'products', id), {
      status: nextStatus,
    });
  };

  const filteredProducts = products.filter((p) => {
    const matchesSearch = p.name.toLowerCase().includes(search.toLowerCase()) || p.sellerName.toLowerCase().includes(search.toLowerCase());
    const matchesFilter = filter === 'all' || p.moderationStatus === filter;
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-on-surface flex items-center gap-2">
            <ShoppingBag className="w-7 h-7 text-primary" />
            Quản Lý & Kiểm Duyệt Sản Phẩm Nông Nghiệp
          </h2>
          <p className="text-sm text-on-surface-variant">Duyệt nông sản, vật tư nông nghiệp và phân bón</p>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex gap-4 items-center bg-surface-low p-4 rounded-xl">
        <div className="relative flex-1">
          <Search className="w-4 h-4 absolute left-3 top-3 text-on-surface-variant" />
          <input
            type="text"
            placeholder="Tìm theo tên sản phẩm, người bán..."
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
          <option value="all">Tất cả trạng thái</option>
          <option value="pending">Chờ duyệt</option>
          <option value="approved">Đã duyệt</option>
          <option value="rejected">Bị từ chối</option>
        </select>
      </div>

      {/* Product Table */}
      <div className="bg-surface-low rounded-xl overflow-hidden border border-outline-variant/20">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-outline-variant/20 text-xs font-semibold text-on-surface-variant uppercase bg-surface">
              <th className="p-4">Sản phẩm</th>
              <th className="p-4">Người bán</th>
              <th className="p-4">Danh mục</th>
              <th className="p-4">Giá / Báo giá</th>
              <th className="p-4">Khu vực</th>
              <th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/10 text-sm">
            {filteredProducts.map((product) => (
              <tr key={product.id} className="hover:bg-surface-high/50">
                <td className="p-4 font-semibold text-on-surface">{product.name}</td>
                <td className="p-4 text-on-surface-variant">{product.sellerName}</td>
                <td className="p-4">{product.category}</td>
                <td className="p-4">
                  {product.saleType === 'fixed_price'
                    ? `${product.listedPrice.toLocaleString('vi-VN')} đ`
                    : `Cần báo giá (TH: ${product.referencePrice.toLocaleString('vi-VN')} đ)`}
                </td>
                <td className="p-4">{product.region}</td>
                <td className="p-4">
                  <span
                    className={`px-2 py-1 text-xs rounded-full font-semibold ${
                      product.moderationStatus === 'approved'
                        ? 'bg-green-500/20 text-green-400'
                        : product.moderationStatus === 'rejected'
                        ? 'bg-red-500/20 text-red-400'
                        : 'bg-yellow-500/20 text-yellow-400'
                    }`}
                  >
                    {product.moderationStatus === 'approved'
                      ? 'Đã duyệt'
                      : product.moderationStatus === 'rejected'
                      ? 'Bị từ chối'
                      : 'Chờ duyệt'}
                  </span>
                </td>
                <td className="p-4 text-right space-x-2">
                  <button
                    onClick={() => handleApprove(product.id)}
                    className="p-1.5 hover:bg-green-500/20 text-green-400 rounded-lg"
                    title="Duyệt sản phẩm"
                  >
                    <CheckCircle className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleReject(product.id)}
                    className="p-1.5 hover:bg-red-500/20 text-red-400 rounded-lg"
                    title="Từ chối sản phẩm"
                  >
                    <XCircle className="w-5 h-5" />
                  </button>
                  <button
                    onClick={() => handleToggleHide(product.id, product.status)}
                    className="p-1.5 hover:bg-yellow-500/20 text-yellow-400 rounded-lg"
                    title="Ẩn / Hiện sản phẩm"
                  >
                    <EyeOff className="w-5 h-5" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
