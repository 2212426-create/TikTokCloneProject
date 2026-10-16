import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

/// Dialog hiển thị ảnh đại diện toàn màn hình khi người dùng ấn vào avatar
/// Port từ FullScreenAvatarActivity.java
class AvatarPreviewDialog extends StatelessWidget {
  final String avatarUrl;

  const AvatarPreviewDialog({
    super.key,
    required this.avatarUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Dialog(
      // ignore: deprecated_member_use
      backgroundColor: Colors.black.withValues(alpha: 0.9),
      insetPadding: EdgeInsets.zero,
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Xem ảnh zoom/fit giữa màn hình
          Center(
            child: InteractiveViewer(
              minScale: 0.5,
              maxScale: 3.0,
              child: avatarUrl.isNotEmpty
                  ? CachedNetworkImage(
                      imageUrl: avatarUrl,
                      placeholder: (context, url) => const CircularProgressIndicator(
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white30),
                      ),
                      errorWidget: (context, url, error) => const Icon(
                        Icons.person,
                        size: 100,
                        color: Colors.white70,
                      ),
                      fit: BoxFit.contain,
                    )
                  : Image.asset(
                      'assets/images/default_avatar.png',
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) => const Icon(
                        Icons.person,
                        size: 100,
                        color: Colors.white70,
                      ),
                    ),
            ),
          ),

          // Nút Đóng góc trên cùng bên trái
          Positioned(
            top: MediaQuery.of(context).padding.top + 16,
            left: 16,
            child: GestureDetector(
              onTap: () => Navigator.of(context).pop(),
              child: Container(
                padding: const EdgeInsets.all(8),
                decoration: const BoxDecoration(
                  color: Colors.black38,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.close,
                  color: Colors.white,
                  size: 24,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
