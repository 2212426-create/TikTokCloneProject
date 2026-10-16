import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Interface cho ThemeRepository theo chuẩn Clean Architecture / MVVM
abstract class ThemeRepository {
  /// Lấy ThemeMode hiện tại từ local storage
  Future<ThemeMode> getThemeMode();

  /// Lưu ThemeMode mới vào local storage
  Future<void> saveThemeMode(ThemeMode mode);
}

/// Triển khai thực tế của ThemeRepository sử dụng SharedPreferences
class ThemeRepositoryImpl implements ThemeRepository {
  static const String _themeKey = 'theme_mode';

  @override
  Future<ThemeMode> getThemeMode() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final themeIndex = prefs.getInt(_themeKey);
      if (themeIndex != null && themeIndex >= 0 && themeIndex < ThemeMode.values.length) {
        return ThemeMode.values[themeIndex];
      }
    } catch (_) {
      // Trả về mặc định hệ thống nếu gặp lỗi đọc file
    }
    return ThemeMode.system;
  }

  @override
  Future<void> saveThemeMode(ThemeMode mode) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt(_themeKey, mode.index);
    } catch (_) {
      // Bỏ qua lỗi
    }
  }
}
