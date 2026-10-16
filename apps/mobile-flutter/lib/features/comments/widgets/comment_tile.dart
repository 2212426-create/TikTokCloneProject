import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';
import '../../profile/providers/profile_provider.dart';
import '../models/comment_model.dart';
import '../providers/comment_provider.dart';

/// Widget hiển thị một dòng bình luận hoặc các nút thao tác đặc biệt (Xem thêm/Thu gọn)
class CommentTile extends ConsumerWidget {
  final CommentModel comment;
  final VoidCallback onReplyTap;
  final VoidCallback onExpandTap;
  final VoidCallback onCollapseTap;

  const CommentTile({
    super.key,
    required this.comment,
    required this.onReplyTap,
    required this.onExpandTap,
    required this.onCollapseTap,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final commentId = comment.commentId;

    // 1. Xử lý các dòng giả Xem thêm / Thu gọn phản hồi
    if (commentId.endsWith('_expand')) {
      final remaining = comment.totalReplies - 2;
      return InkWell(
        onTap: onExpandTap,
        child: Padding(
          padding: const EdgeInsets.only(left: 56, top: 6, bottom: 6),
          child: Text(
            '— Xem thêm $remaining câu trả lời...',
            style: const TextStyle(
              color: Color(0xFF3D85C6),
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      );
    }

    if (commentId.endsWith('_collapse')) {
      return InkWell(
        onTap: onCollapseTap,
        child: Padding(
          padding: const EdgeInsets.only(left: 56, top: 6, bottom: 6),
          child: Text(
            '— Thu gọn câu trả lời',
            style: const TextStyle(
              color: Color(0xFF3D85C6),
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      );
    }

    // 2. Xử lý dòng bình luận bình thường
    final isReply = comment.parentId.isNotEmpty;

    // Lấy thông tin tác giả của bình luận
    final profileAsync = ref.watch(userProfileStreamProvider(comment.authorId));
    final avatarUrl = profileAsync.valueOrNull?.avatarUrl ?? '';
    final username = profileAsync.valueOrNull?.username ?? 'Đang tải...';

    // Theo dõi lượt thích của bình luận
    final likesState = ref.watch(commentLikesStateProvider(commentId));
    final currentUserId = ref.watch(currentUserProvider)?.uid;
    final likedUsers =
        likesState.valueOrNull?['users'] as Map<String, dynamic>? ?? {};
    final isLiked = currentUserId != null && likedUsers[currentUserId] == true;
    final totalLikes =
        likesState.valueOrNull?['count'] as int? ?? comment.totalLikes;

    final isMyComment = currentUserId != null && comment.authorId == currentUserId;

    return GestureDetector(
      onLongPress: isMyComment ? () => _showDeleteDialog(context, ref) : null,
      child: Padding(
        padding: EdgeInsets.only(
          left: isReply ? 52.0 : 12.0,
          right: 12.0,
          top: 8.0,
          bottom: 8.0,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Ảnh đại diện tác giả
            Container(
              width: isReply ? 28 : 36,
              height: isReply ? 28 : 36,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.black26,
              ),
              child: ClipOval(
                child: avatarUrl.isNotEmpty
                    ? CachedNetworkImage(
                        imageUrl: avatarUrl,
                        fit: BoxFit.cover,
                        memCacheWidth: 80, // Tối ưu kích thước lưu cache bộ nhớ
                        placeholder: (context, url) => const Icon(
                          Icons.person,
                          color: Colors.white24,
                          size: 20,
                        ),
                        errorWidget: (context, url, error) => const Icon(
                          Icons.person,
                          color: Colors.white24,
                          size: 20,
                        ),
                      )
                    : const Icon(
                        Icons.person,
                        color: Colors.white54,
                        size: 20,
                      ),
              ),
            ),
            const SizedBox(width: 10),

            // Nội dung bình luận
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Username
                  Text(
                    username,
                    style: TextStyle(
                      color: Colors.grey[400],
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 2),

                  // Nội dung text bình luận (phân biệt tag username cha nếu là câu trả lời)
                  RichText(
                    text: TextSpan(
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        height: 1.3,
                      ),
                      children: [
                        if (isReply && comment.parentUsername.isNotEmpty) ...[
                          TextSpan(
                            text: '@${comment.parentUsername} ',
                            style: const TextStyle(
                              color: Color(0xFF3D85C6),
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                        TextSpan(text: comment.content),
                      ],
                    ),
                  ),
                  const SizedBox(height: 6),

                  // Dòng phản hồi / Thời gian
                  Row(
                    children: [
                      Text(
                        _formatTimestamp(comment.commentId),
                        style: TextStyle(
                          color: Colors.grey[600],
                          fontSize: 11,
                        ),
                      ),
                      const SizedBox(width: 16),
                      GestureDetector(
                        onTap: onReplyTap,
                        child: const Text(
                          'Trả lời',
                          style: TextStyle(
                            color: Colors.grey,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),

            // Nút Like bình luận
            GestureDetector(
              onTap: () {
                if (currentUserId == null) return;
                ref.read(commentRepositoryProvider).toggleCommentLike(
                      commentId: commentId,
                      currentUid: currentUserId,
                      isLiked: !isLiked,
                    );
              },
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    isLiked ? Icons.favorite : Icons.favorite_border,
                    color: isLiked ? AppTheme.primaryColor : Colors.grey[600],
                    size: 18,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    totalLikes.toString(),
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 11,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.grey[900],
        title: const Text('Xóa bình luận', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: const Text('Bạn có chắc chắn muốn xóa bình luận này không?', style: TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(ctx).pop();
              try {
                await ref.read(commentRepositoryProvider).deleteComment(
                      commentId: comment.commentId,
                      videoId: comment.videoId,
                      parentId: comment.parentId.isNotEmpty ? comment.parentId : null,
                    );
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('Lỗi khi xóa bình luận: $e')),
                  );
                }
              }
            },
            child: const Text('Xóa', style: TextStyle(color: AppTheme.primaryColor, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  /// Định dạng timestamp thành dạng ngày tháng đơn giản hoặc "Vừa xong"
  String _formatTimestamp(String timestampStr) {
    final milliseconds = int.tryParse(timestampStr) ?? 0;
    if (milliseconds == 0) return '';
    final commentDate = DateTime.fromMillisecondsSinceEpoch(milliseconds);
    final now = DateTime.now();
    final diff = now.difference(commentDate);

    if (diff.inSeconds < 60) {
      return 'Vừa xong';
    } else if (diff.inMinutes < 60) {
      return '${diff.inMinutes} phút trước';
    } else if (diff.inHours < 24) {
      return '${diff.inHours} giờ trước';
    } else {
      return '${commentDate.day}/${commentDate.month}';
    }
  }
}
