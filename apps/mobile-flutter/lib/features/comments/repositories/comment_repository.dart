// ignore_for_file: prefer_initializing_formals

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_database/firebase_database.dart';
import '../../../core/constants/app_constants.dart';
import '../../video_feed/repositories/video_repository.dart';
import '../models/comment_model.dart';

/// Repository quản lý bình luận trên Firestore & Realtime Database
class CommentRepository {
  final FirebaseFirestore _firestore;
  final FirebaseDatabase _database;

  CommentRepository({
    required FirebaseFirestore firestore,
    required FirebaseDatabase database,
  })  : _firestore = firestore,
        _database = database;

  /// Stream danh sách bình luận thô của một video
  Stream<List<CommentModel>> watchComments(String videoId) {
    return _firestore
        .collection(AppConstants.commentsCollection)
        .where('videoId', isEqualTo: videoId)
        .limit(150)
        .snapshots()
        .map((snapshot) {
      return snapshot.docs
          .map((doc) => CommentModel.fromMap(doc.data()))
          .toList();
    });
  }

  /// Đăng bình luận mới hoặc phản hồi
  Future<void> postComment({
    required CommentModel comment,
    required String authorVideoId,
    required String currentUsername,
    String? repliedCommentAuthorId,
  }) async {
    // 1. Tạo tài liệu bình luận trên Firestore
    await _firestore
        .collection(AppConstants.commentsCollection)
        .doc(comment.commentId)
        .set(comment.toMap());

    // 2. Nếu là câu trả lời (có parentId), cập nhật tài liệu bình luận cha
    if (comment.parentId.isNotEmpty) {
      final parentRef = _firestore
          .collection(AppConstants.commentsCollection)
          .doc(comment.parentId);

      await parentRef.update({
        'totalReplies': FieldValue.increment(1),
        'replyIds': FieldValue.arrayUnion([comment.commentId]),
      });

      // Gửi thông báo đến chủ bình luận cha (nếu người phản hồi không phải là họ)
      if (repliedCommentAuthorId != null &&
          repliedCommentAuthorId.isNotEmpty &&
          repliedCommentAuthorId != comment.authorId) {
        await _pushCommentNotification(
          targetUid: repliedCommentAuthorId,
          fromUsername: currentUsername,
          videoId: comment.videoId,
        );
      }
    }

    // 3. Gửi thông báo bình luận đến chủ video (nếu người bình luận không phải chủ video)
    if (authorVideoId != comment.authorId) {
      await _pushCommentNotification(
        targetUid: authorVideoId,
        fromUsername: currentUsername,
        videoId: comment.videoId,
      );
    }

    // 4. Tăng tổng số bình luận của video trong bảng videos
    await _firestore
        .collection(AppConstants.videosCollection)
        .doc(comment.videoId)
        .update({
      'totalComments': FieldValue.increment(1),
    });

    // 5. Ghi nhận sở thích (Comment weight: 3)
    final videoDoc = await _firestore.collection(AppConstants.videosCollection).doc(comment.videoId).get();
    if (videoDoc.exists) {
      final hashtags = (videoDoc.data()?['hashtags'] as List<dynamic>?)?.map((e) => e.toString()).toList();
      final repo = VideoRepository(firestore: _firestore, database: _database);
      await repo.recordInterest(
        tags: hashtags,
        currentUid: comment.authorId,
        weight: 3,
      );
    }
  }

  /// Gửi thông báo bình luận qua Realtime Database
  Future<void> _pushCommentNotification({
    required String targetUid,
    required String fromUsername,
    required String videoId,
  }) async {
    final notifRef = _database
        .ref(AppConstants.notificationsPath)
        .child(targetUid)
        .push();

    await notifRef.set({
      'fromUsername': fromUsername.isNotEmpty ? fromUsername : 'Ai đó',
      'action': AppConstants.actionComment,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
      'videoId': videoId,
    });
  }

  /// Thích hoặc Bỏ thích bình luận
  Future<void> toggleCommentLike({
    required String commentId,
    required String currentUid,
    required bool isLiked,
  }) async {
    final likeRef = _firestore.collection('comment_likes').doc(commentId);
    final commentRef =
        _firestore.collection(AppConstants.commentsCollection).doc(commentId);

    if (isLiked) {
      // Thêm uid người thích vào document comment_likes/{commentId}
      await likeRef.set({
        currentUid: true,
      }, SetOptions(merge: true));

      // Tăng số lượt thích của bình luận
      await commentRef.update({
        'totalLikes': FieldValue.increment(1),
      });
    } else {
      // Xóa uid người dùng khỏi tài liệu likes
      await likeRef.update({
        currentUid: FieldValue.delete(),
      });

      // Giảm số lượt thích của bình luận
      await commentRef.update({
        'totalLikes': FieldValue.increment(-1),
      });
    }
  }

  /// Stream theo dõi trạng thái Like của bình luận
  Stream<Map<String, dynamic>> watchCommentLikesState(String commentId) {
    return _firestore.collection('comment_likes').doc(commentId).snapshots().map((snapshot) {
      if (!snapshot.exists || snapshot.data() == null) {
        return {'users': <String, dynamic>{}, 'count': 0};
      }
      final data = snapshot.data()!;
      // Đếm số lượng key có giá trị true
      final count = data.values.where((v) => v == true).length;
      return {'users': data, 'count': count};
    });
  }

  /// Xóa bình luận
  Future<void> deleteComment({
    required String commentId,
    required String videoId,
    String? parentId,
  }) async {
    // 1. Xóa tài liệu bình luận trên Firestore
    await _firestore
        .collection(AppConstants.commentsCollection)
        .doc(commentId)
        .delete();

    // 2. Nếu là câu trả lời (có parentId), cập nhật tài liệu bình luận cha
    if (parentId != null && parentId.isNotEmpty) {
      final parentRef = _firestore
          .collection(AppConstants.commentsCollection)
          .doc(parentId);

      await parentRef.update({
        'totalReplies': FieldValue.increment(-1),
        'replyIds': FieldValue.arrayRemove([commentId]),
      });
    }

    // 3. Giảm tổng số bình luận của video trong bảng videos
    await _firestore
        .collection(AppConstants.videosCollection)
        .doc(videoId)
        .update({
      'totalComments': FieldValue.increment(-1),
    });
  }
}
