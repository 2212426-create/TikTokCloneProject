import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/firebase_providers.dart';
import '../models/comment_model.dart';
import '../repositories/comment_repository.dart';

/// Provider cho CommentRepository
final commentRepositoryProvider = Provider<CommentRepository>((ref) {
  return CommentRepository(
    firestore: ref.watch(firestoreProvider),
    database: ref.watch(realtimeDbProvider),
  );
});

/// StreamProvider danh sách bình luận thô theo videoId
final commentsStreamProvider =
    StreamProvider.family<List<CommentModel>, String>((ref, videoId) {
  return ref.watch(commentRepositoryProvider).watchComments(videoId);
});

/// StreamProvider trạng thái Like và tổng số Like của bình luận cụ thể
final commentLikesStateProvider =
    StreamProvider.family<Map<String, dynamic>, String>((ref, commentId) {
  return ref.watch(commentRepositoryProvider).watchCommentLikesState(commentId);
});

/// StateProvider lưu trữ danh sách các ID bình luận cha đang được mở rộng các phản hồi
final expandedCommentsProvider =
    StateProvider.family<Set<String>, String>((ref, videoId) => <String>{});

/// Provider định dạng cấu trúc cây bình luận (gồm phân cấp và các dòng Xem thêm / Thu gọn câu trả lời)
final formattedCommentsProvider =
    Provider.family<List<CommentModel>, String>((ref, videoId) {
  final rawCommentsAsync = ref.watch(commentsStreamProvider(videoId));
  final expandedComments = ref.watch(expandedCommentsProvider(videoId));

  return rawCommentsAsync.when(
    data: (allComments) {
      final List<CommentModel> parents = [];
      final Map<String, List<CommentModel>> repliesMap = {};

      // Phân tách bình luận gốc và câu trả lời
      for (final comment in allComments) {
        if (comment.parentId.isEmpty) {
          parents.add(comment);
        } else {
          repliesMap.putIfAbsent(comment.parentId, () => []).add(comment);
        }
      }

      // Sắp xếp bình luận gốc: mới nhất lên đầu (commentId dạng timestamp giảm dần)
      parents.sort((a, b) {
        final timeA = int.tryParse(a.commentId) ?? 0;
        final timeB = int.tryParse(b.commentId) ?? 0;
        return timeB.compareTo(timeA);
      });

      final List<CommentModel> sorted = [];

      for (final parent in parents) {
        sorted.add(parent);
        final replies = repliesMap[parent.commentId];

        if (replies != null && replies.isNotEmpty) {
          // Sắp xếp các phản hồi: cũ nhất trước (phản hồi theo thứ tự thời gian tăng dần)
          replies.sort((a, b) {
            final timeA = int.tryParse(a.commentId) ?? 0;
            final timeB = int.tryParse(b.commentId) ?? 0;
            return timeA.compareTo(timeB);
          });

          final parentId = parent.commentId;
          final isExpanded = expandedComments.contains(parentId);

          if (isExpanded || replies.length <= 2) {
            // Hiển thị tất cả phản hồi
            sorted.addAll(replies);
            // Nếu có nhiều hơn 2 câu trả lời và đang mở rộng, thêm nút Thu gọn
            if (replies.length > 2) {
              sorted.add(CommentModel(
                commentId: '${parentId}_collapse',
                videoId: videoId,
                authorId: '',
                content: '',
                parentId: parentId,
              ));
            }
          } else {
            // Chỉ hiển thị 2 phản hồi đầu tiên và nút Xem thêm
            sorted.add(replies[0]);
            sorted.add(replies[1]);
            sorted.add(CommentModel(
              commentId: '${parentId}_expand',
              videoId: videoId,
              authorId: '',
              content: '',
              parentId: parentId,
              totalReplies: replies.length, // Truyền tổng số câu trả lời để hiển thị số lượng còn lại
            ));
          }
        }
      }

      return sorted;
    },
    loading: () => [],
    error: (err, stack) => [],
  );
});
