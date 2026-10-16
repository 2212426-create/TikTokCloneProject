import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Placeholder màn hình hộp thư - sẽ triển khai ở Sprint 6
class InboxPlaceholder extends StatelessWidget {
  const InboxPlaceholder({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.inbox_outlined,
              size: 64,
              color: AppTheme.textHint,
            ),
            SizedBox(height: 16),
            Text(
              'Hộp thư',
              style: TextStyle(
                color: AppTheme.textSecondary,
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            SizedBox(height: 8),
            Text(
              'Sẽ triển khai ở Sprint 6',
              style: TextStyle(
                color: AppTheme.textHint,
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
