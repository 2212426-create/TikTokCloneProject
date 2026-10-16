/// Model thông báo - ánh xạ từ Realtime Database node "Notifications/{userId}/{notificationId}"
/// Port từ Notification.java trong Android project
class NotificationModel {
  final String notificationId;
  final String fromUsername;
  final String action; // "0": Follow, "1": Comment, "2": Like, "3": Chat, "APPEAL_REQUEST|..."
  final int timestamp;
  final String? videoId;

  NotificationModel({
    this.notificationId = '',
    required this.fromUsername,
    required this.action,
    required this.timestamp,
    this.videoId,
  });

  factory NotificationModel.fromMap(Map<dynamic, dynamic> map, {String id = ''}) {
    final timestampVal = map['timestamp'];
    final int timestamp = timestampVal is num
        ? timestampVal.toInt()
        : (int.tryParse(timestampVal?.toString() ?? '') ?? 0);

    return NotificationModel(
      notificationId: id,
      fromUsername: map['fromUsername'] as String? ?? '',
      action: map['action'] as String? ?? '',
      timestamp: timestamp,
      videoId: map['videoId'] as String?,
    );
  }

  Map<String, dynamic> toMap() => {
        'fromUsername': fromUsername,
        'action': action,
        'timestamp': timestamp,
        'videoId': videoId,
      };

  NotificationModel copyWith({
    String? notificationId,
    String? fromUsername,
    String? action,
    int? timestamp,
    String? videoId,
  }) {
    return NotificationModel(
      notificationId: notificationId ?? this.notificationId,
      fromUsername: fromUsername ?? this.fromUsername,
      action: action ?? this.action,
      timestamp: timestamp ?? this.timestamp,
      videoId: videoId ?? this.videoId,
    );
  }
}
