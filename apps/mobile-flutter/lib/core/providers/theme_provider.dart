import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../features/settings/repositories/theme_repository.dart';

/// Provider cung cấp thực thể ThemeRepository
final themeRepositoryProvider = Provider<ThemeRepository>((ref) {
  return ThemeRepositoryImpl();
});

/// ThemeViewModel chịu trách nhiệm quản lý trạng thái ThemeMode cho toàn ứng dụng
/// Tuân thủ mô hình MVVM và Clean Architecture
class ThemeViewModel extends StateNotifier<ThemeMode> {
  final ThemeRepository _repository;

  ThemeViewModel(this._repository) : super(ThemeMode.system) {
    _loadTheme();
  }

  /// Tải theme đã lưu khi khởi tạo ViewModel
  Future<void> _loadTheme() async {
    final savedMode = await _repository.getThemeMode();
    state = savedMode;
  }

  /// Thay đổi theme và lưu lại lâu dài
  Future<void> setThemeMode(ThemeMode mode) async {
    if (state == mode) return;
    state = mode;
    await _repository.saveThemeMode(mode);
  }
}

/// Provider quản lý themeMode toàn ứng dụng
/// Đặt tên là themeNotifierProvider để tương thích ngược hoàn toàn với code cũ
final themeNotifierProvider = StateNotifierProvider<ThemeViewModel, ThemeMode>((ref) {
  final repository = ref.watch(themeRepositoryProvider);
  return ThemeViewModel(repository);
});
