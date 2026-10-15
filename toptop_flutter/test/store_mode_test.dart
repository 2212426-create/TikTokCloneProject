import 'package:flutter_test/flutter_test.dart';
import 'package:toptop_flutter/features/store/models/store_model.dart';

void main() {
  group('StoreModel & Dual Mode Permission Tests', () {
    test('StoreModel status and canSell getter', () {
      final pendingStore = StoreModel(
        storeId: 's1',
        ownerId: 'u1',
        storeName: 'Nông sản Hữu Cơ',
        contactName: 'Nguyễn Văn A',
        phone: '0987654321',
        address: 'Đức Trọng',
        region: 'Lâm Đồng',
        status: 'pending',
        operatingStatus: 'active',
        createdAt: 1700000000000,
        updatedAt: 1700000000000,
      );

      expect(pendingStore.isApproved, isFalse);
      expect(pendingStore.canSell, isFalse);

      final approvedStore = StoreModel(
        storeId: 's2',
        ownerId: 'u2',
        storeName: 'Đại lý VTNN Phú Nông',
        contactName: 'Trần Thị B',
        phone: '0912345678',
        address: 'Liên Nghĩa',
        region: 'Lâm Đồng',
        status: 'approved',
        operatingStatus: 'active',
        createdAt: 1700000000000,
        updatedAt: 1700000000000,
      );

      expect(approvedStore.isApproved, isTrue);
      expect(approvedStore.canSell, isTrue);

      final suspendedStore = StoreModel(
        storeId: 's3',
        ownerId: 'u3',
        storeName: 'Nông nghiệp Xanh',
        contactName: 'Lê Văn C',
        phone: '0900000000',
        address: 'Bảo Lộc',
        region: 'Lâm Đồng',
        status: 'approved',
        operatingStatus: 'suspended', // Revoked / suspended operating status
        createdAt: 1700000000000,
        updatedAt: 1700000000000,
      );

      expect(suspendedStore.isApproved, isTrue);
      expect(suspendedStore.isActive, isFalse);
      expect(suspendedStore.canSell, isFalse);
    });

    test('StoreModel serialization and deserialization', () {
      final store = StoreModel(
        storeId: 'store_100',
        ownerId: 'user_100',
        storeType: 'business',
        businessSubtype: 'agent',
        storeName: 'Đại lý VTNN Lâm Đồng',
        contactName: 'Phạm Văn D',
        phone: '0933333333',
        address: 'Đà Lạt',
        region: 'Lâm Đồng',
        taxCode: '0312345678',
        representative: 'Phạm Văn D',
        status: 'approved',
        operatingStatus: 'active',
        createdAt: 1700000000000,
        updatedAt: 1700000000000,
      );

      final map = store.toMap();
      final restored = StoreModel.fromMap(map, 'store_100');

      expect(restored.storeId, equals('store_100'));
      expect(restored.storeType, equals('business'));
      expect(restored.businessSubtype, equals('agent'));
      expect(restored.taxCode, equals('0312345678'));
      expect(restored.canSell, isTrue);
    });
  });
}
