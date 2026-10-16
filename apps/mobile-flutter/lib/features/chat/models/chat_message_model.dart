/// Model tin nhắn chat - ánh xạ từ Realtime Database node "Chats/{roomId}/{messageId}"
/// Port từ ChatMessage.java trong Android project
class ChatMessageModel {
  final String id;
  final String senderId;
  final String receiverId;
  final String message;
  final int timestamp;
  final String type; // "text", "image", "video_share", "system"

  /// metadata dùng cho video_share: {videoId, thumbnailUrl, description, authorUsername, watchCount}
  final Map<String, dynamic>? metadata;

  /// seenBy: {userId: timestamp} - track trạng thái đã xem
  final Map<String, dynamic>? seenBy;

  ChatMessageModel({
    this.id = '',
    required this.senderId,
    required this.receiverId,
    required this.message,
    required this.timestamp,
    this.type = 'text',
    this.metadata,
    this.seenBy,
  });

  /// Kiểm tra tin nhắn đã được người nhận xem chưa
  bool isSeenBy(String userId) => seenBy != null && seenBy!.containsKey(userId);

  factory ChatMessageModel.fromMap(Map<dynamic, dynamic> map, {String id = ''}) {
    final timestampVal = map['timestamp'];
    final int timestamp = timestampVal is num
        ? timestampVal.toInt()
        : (int.tryParse(timestampVal?.toString() ?? '') ?? 0);

    Map<String, dynamic>? metadata;
    if (map['metadata'] != null) {
      try {
        metadata = Map<String, dynamic>.from(map['metadata'] as Map);
      } catch (_) {}
    }

    Map<String, dynamic>? seenBy;
    if (map['seenBy'] != null) {
      try {
        seenBy = Map<String, dynamic>.from(map['seenBy'] as Map);
      } catch (_) {}
    }

    return ChatMessageModel(
      id: id,
      senderId: map['senderId'] as String? ?? '',
      receiverId: map['receiverId'] as String? ?? '',
      message: map['message'] as String? ?? '',
      timestamp: timestamp,
      type: map['type'] as String? ?? 'text',
      metadata: metadata,
      seenBy: seenBy,
    );
  }

  Map<String, dynamic> toMap() => {
        'senderId': senderId,
        'receiverId': receiverId,
        'message': message,
        'timestamp': timestamp,
        'type': type,
        if (metadata != null) 'metadata': metadata,
      };

  ChatMessageModel copyWith({
    String? id,
    String? senderId,
    String? receiverId,
    String? message,
    int? timestamp,
    String? type,
    Map<String, dynamic>? metadata,
    Map<String, dynamic>? seenBy,
  }) {
    return ChatMessageModel(
      id: id ?? this.id,
      senderId: senderId ?? this.senderId,
      receiverId: receiverId ?? this.receiverId,
      message: message ?? this.message,
      timestamp: timestamp ?? this.timestamp,
      type: type ?? this.type,
      metadata: metadata ?? this.metadata,
      seenBy: seenBy ?? this.seenBy,
    );
  }
}
