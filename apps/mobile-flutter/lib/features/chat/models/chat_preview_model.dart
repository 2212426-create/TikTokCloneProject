import 'package:cloud_firestore/cloud_firestore.dart';

class ChatPreview {
  final String id;
  final String title;
  final String? avatarUrl;
  final List<String> memberIds;
  final bool isPinned;
  final bool isMuted;
  final bool isStarred;
  final bool isArchived;
  final bool isRead;
  final String? lastMessage;
  final DateTime? lastMessageAt;

  final bool isGroup;

  ChatPreview({
    required this.id,
    required this.title,
    this.avatarUrl,
    required this.memberIds,
    this.isPinned = false,
    this.isMuted = false,
    this.isStarred = false,
    this.isArchived = false,
    this.isRead = true,
    this.isGroup = false,
    this.lastMessage,
    this.lastMessageAt,
  });

  factory ChatPreview.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return ChatPreview(
      id: doc.id,
      title: data['groupName'] ?? data['title'] ?? 'Chat',
      avatarUrl: data['avatarUrl'],
      memberIds: List<String>.from(data['memberIds'] ?? []),
      isPinned: data['isPinned'] ?? false,
      isMuted: data['isMuted'] ?? false,
      isStarred: data['isStarred'] ?? false,
      isArchived: data['isArchived'] ?? false,
      isRead: data['isRead'] ?? true,
      isGroup: data['isGroup'] ?? false,
      lastMessage: data['lastMessage'],
      lastMessageAt: (data['lastMessageAt'] as Timestamp?)?.toDate(),
    );
  }
}
