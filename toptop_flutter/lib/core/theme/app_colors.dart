import 'package:flutter/material.dart';

/// Design System Color Tokens cho ứng dụng "Nông Video"
/// Cung cấp bảng màu Nông Nghiệp (Xanh lá, Vàng lúa, Nâu đất) đạt tiêu chuẩn tương phản cao WCAG AA
class AppColors {
  AppColors._();

  // ── Brand Colors (Nông Nghiệp) ──────────────────────────────────────────
  static const Color primary = Color(0xFF2E7D32);     // Xanh lá cây nông nghiệp
  static const Color secondary = Color(0xFFFBC02D);   // Vàng lúa / Nông sản
  static const Color accentBrown = Color(0xFF795548); // Nâu đất trồng
  static const Color primaryGradientStart = Color(0xFF2E7D32);
  static const Color primaryGradientEnd = Color(0xFF4CAF50);

  static const LinearGradient brandGradient = LinearGradient(
    colors: [primaryGradientStart, primaryGradientEnd],
    begin: Alignment.centerLeft,
    end: Alignment.centerRight,
  );

  static const LinearGradient yellowGradient = LinearGradient(
    colors: [Color(0xFFFBC02D), Color(0xFFFFA000)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient logoGradient = LinearGradient(
    colors: [Color(0xFF2E7D32), Color(0xFFFBC02D)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient cyanGradient = LinearGradient(
    colors: [Color(0xFF25F4EE), Color(0xFF00C9DB)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  // ── Light Theme Palette ────────────────────────────────────────────────────
  static const Color lightBg = Color(0xFFF6F8F6);
  static const Color lightSurface = Color(0xFFEDF2ED);
  static const Color lightCard = Color(0xFFFFFFFF);
  static const Color lightBorder = Color(0xFFD8E2D8);
  static const Color lightTextPrimary = Color(0xFF1B231B);
  static const Color lightTextSecondary = Color(0xFF5C6B5C);
  static const Color lightTextHint = Color(0xFF94A394);

  // ── Dark Theme Palette ────────────────────────────────────────────────────
  static const Color darkBg = Color(0xFF0F140F);      // Nền tối xanh lá đậm
  static const Color darkSurface = Color(0xFF182218); // Phân vùng phụ
  static const Color darkCard = Color(0xFF222E22);
  static const Color darkBorder = Color(0xFF2F3E2F);
  static const Color darkTextPrimary = Color(0xFFFFFFFF);
  static const Color darkTextSecondary = Color(0xFFA0B2A0);
  static const Color darkTextHint = Color(0xFF566656);

  // ── Status Colors ─────────────────────────────────────────────────────────
  static const Color success = Color(0xFF30D158);
  static const Color warning = Color(0xFFFF9F0A);
  static const Color danger = Color(0xFFFF453A);
  static const Color info = Color(0xFF0A84FF);
}
