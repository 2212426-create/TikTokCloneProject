import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';

/// Màn hình Quản lý Tài khoản (Thay đổi mật khẩu, Xóa tài khoản)
/// Port từ AccountSettingActivity.java trong Android project
class AccountSettingsScreen extends StatelessWidget {
  const AccountSettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: AppTheme.backgroundColor,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Tài khoản',
          style: TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        centerTitle: true,
      ),
      body: ListView(
        physics: const BouncingScrollPhysics(),
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Text(
              'THÔNG TIN TÀI KHOẢN',
              style: TextStyle(
                color: AppTheme.textHint,
                fontSize: 12,
                fontWeight: FontWeight.bold,
                letterSpacing: 0.8,
              ),
            ),
          ),

          // Tùy chọn Thay đổi mật khẩu
          ListTile(
            leading: const Icon(Icons.lock_outline_rounded, color: Colors.white),
            title: const Text(
              'Mật khẩu',
              style: TextStyle(color: Colors.white, fontSize: 15),
            ),
            trailing: const Icon(Icons.chevron_right, color: AppTheme.textHint),
            onTap: () {
              context.push('/settings/password');
            },
          ),

          const Divider(color: AppTheme.dividerColor, height: 32),

          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Text(
              'KIỂM SOÁT TÀI KHOẢN',
              style: TextStyle(
                color: AppTheme.textHint,
                fontSize: 12,
                fontWeight: FontWeight.bold,
                letterSpacing: 0.8,
              ),
            ),
          ),

          // Tùy chọn Xóa tài khoản
          ListTile(
            leading: const Icon(Icons.delete_outline_rounded, color: Color(0xFFFE2C55)),
            title: const Text(
              'Xóa tài khoản',
              style: TextStyle(color: Color(0xFFFE2C55), fontSize: 15, fontWeight: FontWeight.bold),
            ),
            trailing: const Icon(Icons.chevron_right, color: Color(0xFFFE2C55)),
            onTap: () {
              context.push('/settings/delete-account');
            },
          ),
        ],
      ),
    );
  }
}
