import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../auth/providers/auth_provider.dart';
import '../repositories/store_repository.dart';

enum AppMode {
  normal, // Chế độ thông thường (Người xem video, học kỹ thuật, ghi nhật ký, mua hàng)
  seller, // Chế độ bán hàng (Tổng quan, Sản phẩm, Đơn bán, Tin nhắn, Cài đặt gian hàng)
}

final appModeProvider = StateNotifierProvider<AppModeNotifier, AppMode>((ref) {
  final currentUser = ref.watch(currentUserProvider);
  final storeRepo = ref.watch(storeRepositoryProvider);
  return AppModeNotifier(ref: ref, userId: currentUser?.uid, storeRepo: storeRepo);
});

class AppModeNotifier extends StateNotifier<AppMode> {
  final Ref ref;
  final String? userId;
  final StoreRepository storeRepo;

  AppModeNotifier({
    required this.ref,
    required this.userId,
    required this.storeRepo,
  }) : super(AppMode.normal) {
    _initMode();
  }

  Future<void> _initMode() async {
    if (userId == null || userId!.isEmpty) {
      state = AppMode.normal;
      return;
    }

    final prefs = await SharedPreferences.getInstance();
    final savedMode = prefs.getString('app_mode_$userId');

    if (savedMode == 'seller') {
      // Xác minh quyền bán hàng thực tế từ backend/Firestore trước khi khôi phục
      final store = await storeRepo.getStoreByOwner(userId!);
      if (store != null && store.canSell) {
        state = AppMode.seller;
      } else {
        // Quyền bị thu hồi hoặc tạm ngưng -> Tự động chuyển về chế độ thông thường
        state = AppMode.normal;
        await prefs.setString('app_mode_$userId', 'normal');
      }
    } else {
      state = AppMode.normal;
    }
  }

  /// Chuyển giao diện sang Chế độ bán hàng (Chỉ thành công nếu gian hàng đã được duyệt)
  Future<bool> switchToSellerMode() async {
    if (userId == null || userId!.isEmpty) return false;

    final store = await storeRepo.getStoreByOwner(userId!);
    if (store != null && store.canSell) {
      state = AppMode.seller;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('app_mode_$userId', 'seller');
      return true;
    }

    // Không đủ quyền -> Revert
    state = AppMode.normal;
    return false;
  }

  /// Chuyển giao diện về Chế độ thông thường
  Future<void> switchToNormalMode() async {
    state = AppMode.normal;
    if (userId != null && userId!.isNotEmpty) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('app_mode_$userId', 'normal');
    }
  }
}
