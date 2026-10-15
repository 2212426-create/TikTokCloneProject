import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/firebase_providers.dart';
import '../models/store_model.dart';

final storeRepositoryProvider = Provider<StoreRepository>((ref) {
  return StoreRepository(firestore: ref.watch(firestoreProvider));
});

final userStoreStreamProvider = StreamProvider.family<StoreModel?, String>((ref, ownerId) {
  return ref.watch(storeRepositoryProvider).watchUserStore(ownerId);
});

final publicStoreStreamProvider = StreamProvider.family<StoreModel?, String>((ref, storeId) {
  return ref.watch(storeRepositoryProvider).watchPublicStore(storeId);
});

class StoreRepository {
  final FirebaseFirestore _firestore;

  StoreRepository({required FirebaseFirestore firestore}) : _firestore = firestore;

  /// Stream thông tin gian hàng của user (Mỗi tài khoản tối đa 1 gian hàng)
  Stream<StoreModel?> watchUserStore(String ownerId) {
    if (ownerId.isEmpty) return Stream.value(null);
    return _firestore
        .collection('stores')
        .where('ownerId', isEqualTo: ownerId)
        .snapshots()
        .map((snapshot) {
      if (snapshot.docs.isEmpty) return null;
      final doc = snapshot.docs.first;
      return StoreModel.fromMap(doc.data(), doc.id);
    });
  }

  /// Lấy thông tin gian hàng theo ownerId bằng Future
  Future<StoreModel?> getStoreByOwner(String ownerId) async {
    if (ownerId.isEmpty) return null;
    final snapshot = await _firestore
        .collection('stores')
        .where('ownerId', isEqualTo: ownerId)
        .limit(1)
        .get();

    if (snapshot.docs.isEmpty) return null;
    final doc = snapshot.docs.first;
    return StoreModel.fromMap(doc.data(), doc.id);
  }

  /// Stream gian hàng công khai theo storeId
  Stream<StoreModel?> watchPublicStore(String storeId) {
    if (storeId.isEmpty) return Stream.value(null);
    return _firestore.collection('stores').doc(storeId).snapshots().map((doc) {
      if (!doc.exists) return null;
      return StoreModel.fromMap(doc.data()!, doc.id);
    });
  }

  /// Lưu bản nháp hồ sơ đăng ký gian hàng
  Future<String> saveStoreDraft(StoreModel store) async {
    final docRef = store.storeId.isNotEmpty
        ? _firestore.collection('stores').doc(store.storeId)
        : _firestore.collection('stores').doc();

    final now = DateTime.now().millisecondsSinceEpoch;
    final map = store.toMap();
    map['storeId'] = docRef.id;
    map['status'] = 'draft';
    map['updatedAt'] = now;
    if (store.createdAt == 0) map['createdAt'] = now;

    await docRef.set(map, SetOptions(merge: true));
    return docRef.id;
  }

  /// Gửi hồ sơ đăng ký gian hàng lên chờ xét duyệt (pending)
  Future<String> submitStoreRegistration(StoreModel store) async {
    final docRef = store.storeId.isNotEmpty
        ? _firestore.collection('stores').doc(store.storeId)
        : _firestore.collection('stores').doc();

    final now = DateTime.now().millisecondsSinceEpoch;
    final map = store.toMap();
    map['storeId'] = docRef.id;
    map['status'] = 'pending';
    map['rejectionReason'] = null;
    map['updatedAt'] = now;
    if (store.createdAt == 0) map['createdAt'] = now;

    await docRef.set(map, SetOptions(merge: true));
    return docRef.id;
  }

  /// Cập nhật thông tin gian hàng đã được duyệt
  Future<void> updateStoreSettings(String storeId, Map<String, dynamic> data) async {
    data['updatedAt'] = DateTime.now().millisecondsSinceEpoch;
    await _firestore.collection('stores').doc(storeId).update(data);
  }
}
