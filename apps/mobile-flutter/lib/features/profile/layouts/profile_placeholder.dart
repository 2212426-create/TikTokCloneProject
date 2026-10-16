import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';

/// Placeholder màn hình hồ sơ cá nhân
/// Hiển thị thông tin cơ bản + nút đăng xuất
/// Sẽ triển khai đầy đủ ở Sprint 3
class ProfilePlaceholder extends ConsumerWidget {
  const ProfilePlaceholder({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(currentUserProvider);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Avatar placeholder
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppTheme.surfaceColor,
                border: Border.all(
                  color: AppTheme.textHint.withValues(alpha: 0.3),
                ),
              ),
              child: const Icon(
                Icons.person,
                size: 40,
                color: AppTheme.textHint,
              ),
            ),
            const SizedBox(height: 16),

            // Email
            if (user != null) ...[
              Text(
                user.email ?? 'Không có email',
                style: const TextStyle(
                  color: AppTheme.textPrimary,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                'UID: ${user.uid.substring(0, 8)}...',
                style: const TextStyle(
                  color: AppTheme.textHint,
                  fontSize: 12,
                ),
              ),
            ] else
              const Text(
                'Chưa đăng nhập',
                style: TextStyle(
                  color: AppTheme.textSecondary,
                  fontSize: 16,
                ),
              ),

            const SizedBox(height: 8),
            const Text(
              'Hồ sơ đầy đủ sẽ ở Sprint 3',
              style: TextStyle(
                color: AppTheme.textHint,
                fontSize: 14,
              ),
            ),

            const SizedBox(height: 32),

            // Nút đăng xuất
            if (user != null)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 48),
                child: OutlinedButton.icon(
                  onPressed: () async {
                    await ref.read(authNotifierProvider.notifier).signOut();
                    if (context.mounted) {
                      context.go('/auth');
                    }
                  },
                  icon: const Icon(Icons.logout, size: 18),
                  label: const Text('Đăng xuất'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppTheme.primaryColor,
                    side: const BorderSide(color: AppTheme.primaryColor),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
