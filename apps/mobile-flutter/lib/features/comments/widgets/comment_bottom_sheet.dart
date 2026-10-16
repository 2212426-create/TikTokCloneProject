import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';
import '../../profile/providers/profile_provider.dart';
import '../models/comment_model.dart';
import '../providers/comment_provider.dart';
import 'comment_tile.dart';

/// Bottom Sheet hiển thị danh sách bình luận và cho phép viết bình luận/phản hồi
class CommentBottomSheet extends ConsumerStatefulWidget {
  final String videoId;
  final String authorVideoId;

  const CommentBottomSheet({
    super.key,
    required this.videoId,
    required this.authorVideoId,
  });

  @override
  ConsumerState<CommentBottomSheet> createState() => _CommentBottomSheetState();
}

class _CommentBottomSheetState extends ConsumerState<CommentBottomSheet> {
  final TextEditingController _commentController = TextEditingController();
  final FocusNode _focusNode = FocusNode();

  CommentModel? _replyingToComment;
  String? _replyingToUsername;

  @override
  void dispose() {
    _commentController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _startReply(CommentModel comment, String authorUsername) {
    setState(() {
      _replyingToComment = comment;
      _replyingToUsername = authorUsername;
    });
    _focusNode.requestFocus();
  }

  void _cancelReply() {
    setState(() {
      _replyingToComment = null;
      _replyingToUsername = null;
    });
  }

  Future<void> _sendComment(String currentUserId, String currentUsername) async {
    final content = _commentController.text.trim();
    if (content.isEmpty) return;

    final commentId = DateTime.now().millisecondsSinceEpoch.toString();
    
    String parentId = '';
    String parentUsername = '';

    if (_replyingToComment != null) {
      if (_replyingToComment!.parentId.isNotEmpty) {
        parentId = _replyingToComment!.parentId;
      } else {
        parentId = _replyingToComment!.commentId;
      }
      parentUsername = _replyingToUsername ?? '';
    }

    final newComment = CommentModel(
      commentId: commentId,
      videoId: widget.videoId,
      authorId: currentUserId,
      content: content,
      parentId: parentId,
      parentUsername: parentUsername,
    );

    _commentController.clear();
    final repliedAuthorId = _replyingToComment?.authorId;
    _cancelReply();
    _focusNode.unfocus();

    try {
      await ref.read(commentRepositoryProvider).postComment(
            comment: newComment,
            authorVideoId: widget.authorVideoId,
            currentUsername: currentUsername,
            repliedCommentAuthorId: repliedAuthorId,
          );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể gửi bình luận: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final currentUser = ref.watch(currentUserProvider);
    final userProfile = ref.watch(currentUserProfileProvider).valueOrNull;

    if (currentUser == null) {
      return const SizedBox.shrink();
    }

    final myAvatarUrl = userProfile?.avatarUrl ?? '';
    final myUsername =
        userProfile?.username ?? currentUser.email?.split('@')[0] ?? '';

    final formattedComments = ref.watch(formattedCommentsProvider(widget.videoId));
    final commentsAsync = ref.watch(commentsStreamProvider(widget.videoId));

    final totalComments = commentsAsync.valueOrNull?.length ?? 0;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Container(
        height: MediaQuery.of(context).size.height * 0.75,
        decoration: const BoxDecoration(
          color: Color(0xFF161618),
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: Column(
          children: [
            const SizedBox(height: 10),
            Container(
              width: 36,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey[700],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 12),

            // Tiêu đề & Nút đóng
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const SizedBox(width: 24),
                  Text(
                    '$totalComments bình luận',
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                  GestureDetector(
                    onTap: () => Navigator.pop(context),
                    child: const Icon(
                      Icons.close,
                      color: Colors.white,
                      size: 20,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            const Divider(color: Colors.white12, height: 1),

            // Danh sách bình luận
            Expanded(
              child: commentsAsync.when(
                data: (allComments) {
                  if (allComments.isEmpty) {
                    return const Center(
                      child: Text(
                        'Chưa có bình luận nào.\nHãy là người đầu tiên bình luận!',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Colors.grey,
                          fontSize: 14,
                        ),
                      ),
                    );
                  }

                  return ListView.builder(
                    itemCount: formattedComments.length,
                    itemBuilder: (context, index) {
                      final comment = formattedComments[index];
                      return CommentTile(
                        comment: comment,
                        onReplyTap: () {
                          ref
                              .read(userProfileStreamProvider(comment.authorId)
                                  .future)
                              .then((profile) {
                            if (profile != null) {
                              _startReply(comment, profile.username);
                            }
                          });
                        },
                        onExpandTap: () {
                          ref
                              .read(expandedCommentsProvider(widget.videoId)
                                  .notifier)
                              .update((state) => {...state, comment.parentId});
                        },
                        onCollapseTap: () {
                          ref
                              .read(expandedCommentsProvider(widget.videoId)
                                  .notifier)
                              .update((state) {
                            final newState = {...state};
                            newState.remove(comment.parentId);
                            return newState;
                          });
                        },
                      );
                    },
                  );
                },
                loading: () => const Center(
                  child: CircularProgressIndicator(
                    color: AppTheme.primaryColor,
                  ),
                ),
                error: (error, _) => Center(
                  child: Text(
                    'Đã xảy ra lỗi khi tải bình luận: $error',
                    style: const TextStyle(color: Colors.red),
                  ),
                ),
              ),
            ),

            const Divider(color: Colors.white12, height: 1),
            
            // Preview đang phản hồi
            if (_replyingToComment != null)
              Container(
                color: Colors.black26,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Đang trả lời @$_replyingToUsername',
                      style: TextStyle(
                        color: Colors.grey[400],
                        fontSize: 12,
                      ),
                    ),
                    GestureDetector(
                      onTap: _cancelReply,
                      child: const Icon(
                        Icons.close,
                        color: Colors.grey,
                        size: 16,
                      ),
                    ),
                  ],
                ),
              ),

            SafeArea(
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12.0,
                  vertical: 8.0,
                ),
                color: const Color(0xFF1D1D1F),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 18,
                      backgroundColor: Colors.grey[800],
                      backgroundImage: myAvatarUrl.isNotEmpty
                          ? CachedNetworkImageProvider(myAvatarUrl, maxWidth: 80)
                          : null,
                      child: myAvatarUrl.isEmpty
                          ? const Icon(Icons.person, color: Colors.white, size: 18)
                          : null,
                    ),
                    const SizedBox(width: 12),

                    // Ô nhập bình luận - Đảm bảo filled: false và cursorColor rõ nét
                    Expanded(
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.grey[900],
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: TextField(
                          controller: _commentController,
                          focusNode: _focusNode,
                          cursorColor: AppColors.primary,
                          style: const TextStyle(color: Colors.white, fontSize: 14),
                          decoration: InputDecoration(
                            hintText: _replyingToComment != null
                                ? 'Trả lời @$_replyingToUsername...'
                                : 'Thêm bình luận...',
                            hintStyle: const TextStyle(
                              color: Colors.grey,
                              fontSize: 14,
                            ),
                            contentPadding: const EdgeInsets.symmetric(
                              horizontal: 16.0,
                              vertical: 10.0,
                            ),
                            border: InputBorder.none,
                            enabledBorder: InputBorder.none,
                            focusedBorder: InputBorder.none,
                            filled: false, // Bắt buộc false để không bị ghi đè màu trắng từ Light ThemeData
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),

                    ValueListenableBuilder<TextEditingValue>(
                      valueListenable: _commentController,
                      builder: (context, value, child) {
                        final isEmpty = value.text.trim().isEmpty;
                        return IconButton(
                          icon: Icon(
                            Icons.send,
                            color: isEmpty ? Colors.grey : AppTheme.primaryColor,
                            size: 24,
                          ),
                          onPressed: isEmpty
                              ? null
                              : () => _sendComment(currentUser.uid, myUsername),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
