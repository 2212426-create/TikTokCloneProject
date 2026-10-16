class TimeUtils {
  /// Chuyển đổi timestamp sang chuỗi thời gian tương đối (VD: 2giờ, 3ngày, ...)
  static String formatRelativeTime(int timestampMs) {
    final now = DateTime.now().millisecondsSinceEpoch;
    final diff = now - timestampMs;

    if (diff < 60000) return 'Vừa xong';
    if (diff < 3600000) return '${(diff / 60000).floor()}ph';
    if (diff < 86400000) return '${(diff / 3600000).floor()}giờ';
    if (diff < 604800000) return '${(diff / 86400000).floor()}ngày';

    final date = DateTime.fromMillisecondsSinceEpoch(timestampMs);
    return '${date.day}/${date.month}/${date.year}';
  }
}
