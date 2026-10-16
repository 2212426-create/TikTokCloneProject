import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../../comments/widgets/comment_bottom_sheet.dart';
import '../models/video_model.dart';
import '../widgets/video_action_bar.dart';
import '../widgets/video_overlay.dart';
import '../widgets/video_player_widget.dart';

/// Màn hình hiển thị một Video cụ thể (dành cho Deep Link hoặc khi click từ Profile)
/// Port từ VideoActivity.java
class SingleVideoScreen extends ConsumerStatefulWidget {
  final String videoId;
  final bool showComments;
  final bool isEmbedded; // Nếu true, không hiển thị Scaffold và nút back riêng

  const SingleVideoScreen({
    super.key,
    required this.videoId,
    this.showComments = false,
    this.isEmbedded = false,
  });

  @override
  ConsumerState<SingleVideoScreen> createState() => _SingleVideoScreenState();
}

class _SingleVideoScreenState extends ConsumerState<SingleVideoScreen> {
  bool _hasShownComments = false;

  void _triggerComments(BuildContext context, String videoId, String authorId) {
    if (_hasShownComments) return;
    _hasShownComments = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      showModalBottomSheet(
        context: context,
        isScrollControlled: true,
        backgroundColor: Colors.transparent,
        builder: (context) {
          return CommentBottomSheet(
            videoId: videoId,
            authorVideoId: authorId,
          );
        },
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: StreamBuilder<DocumentSnapshot<Map<String, dynamic>>>(
        stream: FirebaseFirestore.instance
            .collection(AppConstants.videosCollection)
            .doc(widget.videoId)
            .snapshots(),
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return Center(
              child: Text(
                'Lỗi: ${snapshot.error}',
                style: const TextStyle(color: Colors.white70),
              ),
            );
          }

          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
              ),
            );
          }

          final doc = snapshot.data;
          if (doc == null || !doc.exists || doc.data() == null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 64, color: Colors.grey),
                  const SizedBox(height: 16),
                  const Text(
                    'Video này đã bị gỡ bỏ hoặc không tồn tại.',
                    style: TextStyle(color: Colors.white70, fontSize: 16),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: () => context.pop(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primaryColor,
                    ),
                    child: const Text('Quay lại', style: TextStyle(color: Colors.white)),
                  ),
                ],
              ),
            );
          }

          final video = VideoModel.fromMap(doc.data()!, doc.id);

          if (widget.showComments) {
            _triggerComments(context, video.videoId, video.authorId);
          }

          if (video.moderationStatus == 'rejected') {
            return _buildErrorState(
              icon: Icons.block,
              message: 'Video này đã bị khóa do vi phạm tiêu chuẩn cộng đồng.',
              context: context,
            );
          }

          final content = Stack(
            fit: StackFit.expand,
            children: [
              // 1. Trình phát Video
              VideoPlayerWidget(video: video, autoPlay: true),

              // 2. Overlay thông tin tác giả và mô tả
              VideoOverlay(video: video),

              // 3. Sidebar tương tác
              VideoActionBar(video: video),

              // 4. Nút Back (Quay lại)
              if (!widget.isEmbedded)
                Positioned(
                  top: MediaQuery.of(context).padding.top + 12,
                  left: 12,
                  child: GestureDetector(
                    onTap: () => context.pop(),
                    child: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: const BoxDecoration(
                        color: Colors.black45,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.arrow_back,
                        color: Colors.white,
                        size: 24,
                      ),
                    ),
                  ),
                ),
            ],
          );

          return widget.isEmbedded ? content : Scaffold(
            backgroundColor: Colors.black,
            body: content,
          );
        },
      ),
    );
  }

  Widget _buildErrorState({required IconData icon, required String message, required BuildContext context}) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 64, color: Colors.grey),
          const SizedBox(height: 16),
          Text(message, style: const TextStyle(color: Colors.white70, fontSize: 16), textAlign: TextAlign.center),
          const SizedBox(height: 24),
          if (!widget.isEmbedded)
            ElevatedButton(
              onPressed: () => context.pop(),
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryColor),
              child: const Text('Quay lại', style: TextStyle(color: Colors.white)),
            ),
        ],
      ),
    );
  }
}
