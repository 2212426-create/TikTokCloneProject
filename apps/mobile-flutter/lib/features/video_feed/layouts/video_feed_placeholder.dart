import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Placeholder màn hình Feed Video - sẽ triển khai đầy đủ ở Sprint 2
class VideoFeedPlaceholder extends StatelessWidget {
  const VideoFeedPlaceholder({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.play_circle_outline,
              size: 64,
              color: AppTheme.textHint,
            ),
            SizedBox(height: 16),
            Text(
              'Video Feed',
              style: TextStyle(
                color: AppTheme.textSecondary,
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
            SizedBox(height: 8),
            Text(
              'Sẽ triển khai ở Sprint 2',
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
