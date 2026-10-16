/// Model bình luận - ánh xạ từ Firestore collection "comments"
/// Port từ Comment.java trong Android project
class CommentModel {
  final String commentId;
  final String videoId;
  final String authorId;
  final String content;
  final int totalLikes;
  final int totalReplies;
  final List<String> replyIds;
  final String parentId;
  final String parentUsername;

  CommentModel({
    required this.commentId,
    required this.videoId,
    required this.authorId,
    required this.content,
    this.totalLikes = 0,
    this.totalReplies = 0,
    this.replyIds = const [],
    this.parentId = '',
    this.parentUsername = '',
  });

  factory CommentModel.fromMap(Map<String, dynamic> map) {
    return CommentModel(
      commentId: map['commentId'] as String? ?? '',
      videoId: map['videoId'] as String? ?? '',
      authorId: map['authorId'] as String? ?? '',
      content: map['content'] as String? ?? '',
      totalLikes: (map['totalLikes'] as num?)?.toInt() ?? 0,
      totalReplies: (map['totalReplies'] as num?)?.toInt() ?? 0,
      replyIds: (map['replyIds'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      parentId: map['parentId'] as String? ?? '',
      parentUsername: map['parentUsername'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() => {
        'commentId': commentId,
        'videoId': videoId,
        'authorId': authorId,
        'content': content,
        'totalLikes': totalLikes,
        'totalReplies': totalReplies,
        'replyIds': replyIds,
        'parentId': parentId,
        'parentUsername': parentUsername,
      };

  CommentModel copyWith({
    String? commentId,
    String? videoId,
    String? authorId,
    String? content,
    int? totalLikes,
    int? totalReplies,
    List<String>? replyIds,
    String? parentId,
    String? parentUsername,
  }) {
    return CommentModel(
      commentId: commentId ?? this.commentId,
      videoId: videoId ?? this.videoId,
      authorId: authorId ?? this.authorId,
      content: content ?? this.content,
      totalLikes: totalLikes ?? this.totalLikes,
      totalReplies: totalReplies ?? this.totalReplies,
      replyIds: replyIds ?? this.replyIds,
      parentId: parentId ?? this.parentId,
      parentUsername: parentUsername ?? this.parentUsername,
    );
  }
}
