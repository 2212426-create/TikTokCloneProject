import 'package:cached_network_image/cached_network_image.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../models/chat_message_model.dart';
import 'video_share_card.dart';

/// Widget hiển thị bong bóng tin nhắn (trái/phải)
/// Port từ ChatAdapter.java trong Android project
class ChatBubble extends StatelessWidget {
  final ChatMessageModel message;
  final bool isMe;
  final String otherUserAvatar;
  final bool showSeenAvatar;

  /// ID của người dùng hiện tại (để check seen status)
  final String? currentUid;

  const ChatBubble({
    super.key,
    required this.message,
    required this.isMe,
    required this.otherUserAvatar,
    this.showSeenAvatar = false,
    this.currentUid,
  });

  /// Hiển thị Dialog kháng cáo
  void _showAppealDialog(BuildContext context, String reportId) {
    final controller = TextEditingController();
    final docRef = FirebaseFirestore.instance
        .collection(AppConstants.reportsCollection)
        .doc(reportId);

    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final onSurfaceColor = theme.colorScheme.onSurface;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: theme.colorScheme.surface,
        title: Text(
          'Phản ánh báo cáo',
          style: TextStyle(color: onSurfaceColor, fontWeight: FontWeight.bold),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Nhập nội dung phản ánh của bạn...',
              style: TextStyle(
                color: isDark ? AppTheme.textSecondary : Colors.black54,
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: controller,
              maxLines: 3,
              style: TextStyle(color: onSurfaceColor, fontSize: 14),
              decoration: InputDecoration(
                hintText: 'Nhập nội dung phản ánh...',
                hintStyle: TextStyle(
                  color: isDark ? AppTheme.textHint : Colors.grey[500],
                ),
                filled: true,
                fillColor: isDark ? const Color(0xFF2C2C2E) : const Color(0xFFF0F0F0),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: BorderSide(
                    color: isDark ? AppTheme.dividerColor : const Color(0xFFE3E3E4),
                  ),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(8),
                  borderSide: const BorderSide(color: Color(0xFFFE2C55), width: 1.5),
                ),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFFE2C55),
              foregroundColor: Colors.white,
              minimumSize: const Size(80, 36),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
            ),
            onPressed: () {
              final content = controller.text.trim();
              if (content.isNotEmpty) {
                docRef.update({'appeal': content}).then((_) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                          content: Text('Đã gửi phản ánh thành công!')),
                    );
                    Navigator.pop(context);
                  }
                }).catchError((error) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Lỗi: $error')),
                    );
                  }
                });
              }
            },
            child: const Text('Gửi', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }

  /// Phân tích nội dung tin nhắn và trả về widget phù hợp
  Widget _buildMessageContent(BuildContext context) {
    final text = message.message;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // ── 1. Video Share Card ─────────────────────────────────────────────
    if (message.type == 'video_share') {
      final videoId = message.metadata?['videoId'] as String? ?? '';
      if (videoId.isNotEmpty) {
        // Xác định roomId để khi bấm vào có thể vuốt dọc được
        String? roomId;
        if (currentUid != null) {
          // Logic Heuristic: Nếu receiverId là UID (1-1), tính RoomId composite.
          // Nếu là Group (thường ngắn hơn hoặc prefix group_), dùng trực tiếp.
          if (message.receiverId.length >= 20 && !message.receiverId.startsWith('group')) {
             roomId = (currentUid!.compareTo(message.receiverId) < 0)
                 ? '${currentUid}_${message.receiverId}' 
                 : '${message.receiverId}_$currentUid';
          } else {
             roomId = message.receiverId; 
          }
        }
        return VideoShareCard(videoId: videoId, roomId: roomId);
      }
    }

    // ── 2. Share URL dạng text (link mới và link toptop-share-page cũ) ──
    for (final sharePrefix in AppConstants.shareUrlPrefixes) {
      if (!text.contains(sharePrefix)) continue;
      final startIdx = text.indexOf(sharePrefix);
      int endIdx = text.indexOf(' ', startIdx);
      if (endIdx == -1) endIdx = text.length;
      final videoId =
          text.substring(startIdx + sharePrefix.length, endIdx).trim();

      if (videoId.isNotEmpty) {
        return Column(
          crossAxisAlignment:
              isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            if (text.substring(0, startIdx).trim().isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(bottom: 6),
                child: Text(
                  text.substring(0, startIdx).trim(),
                  style: TextStyle(
                    color: isMe
                        ? Colors.white
                        : (isDark ? Colors.white : Colors.black87),
                    fontSize: 14,
                  ),
                ),
              ),
            VideoShareCard(videoId: videoId),
          ],
        );
      }
    }

    // ── 3. Appeal message ───────────────────────────────────────────────
    if (text.contains('[APPEAL:') && text.endsWith(']')) {
      final appealIdx = text.indexOf(' [APPEAL:');
      final displayText = text.substring(0, appealIdx);
      final reportId =
          text.substring(text.indexOf(':') + 1, text.length - 1);
      const clickKeyword = 'nhấn vào đây';
      final keywordIdx = displayText.indexOf(clickKeyword);

      if (keywordIdx != -1) {
        final preText = displayText.substring(0, keywordIdx);
        final postText =
            displayText.substring(keywordIdx + clickKeyword.length);
        return RichText(
          text: TextSpan(
            style: const TextStyle(color: Colors.white, fontSize: 14),
            children: [
              TextSpan(text: preText),
              TextSpan(
                text: clickKeyword,
                style: const TextStyle(
                  color: Colors.blue,
                  fontWeight: FontWeight.bold,
                  decoration: TextDecoration.underline,
                ),
                recognizer: TapGestureRecognizer()
                  ..onTap = () => _showAppealDialog(context, reportId),
              ),
              TextSpan(text: postText),
            ],
          ),
        );
      }
      return Text(displayText,
          style: const TextStyle(color: Colors.white, fontSize: 14));
    }

    // ── 4. Sticker message ──────────────────────────────────────────────
    if (message.type == 'sticker') {
      return CachedNetworkImage(
        imageUrl: text,
        height: 100,
        width: 100,
        fit: BoxFit.contain,
      );
    }

    // ── 5. Image message ────────────────────────────────────────────────
    if (message.type == 'image') {
      return GestureDetector(
        onTap: () {
          // Full screen preview logic could go here
        },
        child: Container(
          constraints: const BoxConstraints(
            minWidth: 100,
            minHeight: 100,
            maxWidth: 240,
            maxHeight: 320,
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: CachedNetworkImage(
              imageUrl: text,
              placeholder: (context, url) => Container(
                width: 150,
                height: 150,
                color: isDark ? Colors.grey[900] : Colors.grey[200],
                child: const Center(child: CircularProgressIndicator()),
              ),
              errorWidget: (context, url, error) => const Icon(Icons.error),
              fit: BoxFit.cover,
            ),
          ),
        ),
      );
    }

    // ── 6. Tin nhắn văn bản thông thường ───────────────────────────────
    return Text(
      text,
      style: TextStyle(
        color: isMe ? Colors.white : (isDark ? Colors.white : Colors.black87),
        fontSize: 14,
      ),
    );
  }

  /// Timestamp ngắn gọn (giờ:phút)
  String _formatTime(int timestamp) {
    final dt = DateTime.fromMillisecondsSinceEpoch(timestamp);
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  /// Widget Read Receipt (tick xanh / xám hoặc avatar đối phương khi đã đọc)
  Widget _buildReadReceipt(BuildContext context) {
    if (!isMe) return const SizedBox.shrink();

    final receiverId = message.receiverId;
    final isSeen = message.isSeenBy(receiverId);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Padding(
      padding: const EdgeInsets.only(top: 3, right: 2),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Timestamp nhỏ
          Text(
            _formatTime(message.timestamp),
            style: TextStyle(
              color: isDark ? Colors.white30 : Colors.black38,
              fontSize: 10,
            ),
          ),
          const SizedBox(width: 6),
          // Nếu là tin nhắn đã xem mới nhất -> Hiển thị avatar tròn nhỏ của đối phương
          if (showSeenAvatar && otherUserAvatar.isNotEmpty)
            Container(
              width: 14,
              height: 14,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: isDark ? Colors.black : Colors.white,
                  width: 1.0,
                ),
              ),
              child: ClipOval(
                child: CachedNetworkImage(
                  imageUrl: otherUserAvatar,
                  fit: BoxFit.cover,
                  errorWidget: (context, url, error) => Container(
                    color: Colors.grey,
                    child: const Icon(Icons.person, size: 8, color: Colors.white),
                  ),
                ),
              ),
            )
          else if (showSeenAvatar) // Nếu avatar trống nhưng cần hiển thị avatar seen
            Container(
              width: 14,
              height: 14,
              decoration: BoxDecoration(
                color: const Color(0xFFFE2C55).withValues(alpha: 0.15),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.person, size: 8, color: Color(0xFFFE2C55)),
            )
          else if (!isSeen) // Nếu chưa xem -> hiển thị double tick xám nhạt (sent)
            SizedBox(
              width: 18,
              height: 14,
              child: Stack(
                children: [
                  Icon(
                    Icons.done,
                    size: 14,
                    color: isDark ? Colors.white54 : Colors.black26,
                  ),
                  Positioned(
                    left: 4,
                    child: Icon(
                      Icons.done,
                      size: 14,
                      color: isDark ? Colors.white54 : Colors.black26,
                    ),
                  ),
                ],
              ),
            )
          else // Tin nhắn đã xem cũ -> chỉ cần double tick xanh nhỏ hoặc không hiển thị gì (ẩn đi để tăng tính tối giản)
            SizedBox(
              width: 18,
              height: 14,
              child: Stack(
                children: [
                  Icon(
                    Icons.done,
                    size: 14,
                    color: isDark ? const Color(0xFF25F4EE) : const Color(0xFF0095F6),
                  ),
                  Positioned(
                    left: 4,
                    child: Icon(
                      Icons.done,
                      size: 14,
                      color: isDark ? const Color(0xFF25F4EE) : const Color(0xFF0095F6),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  bool get _isVideoShare =>
      message.type == 'video_share' ||
      AppConstants.shareUrlPrefixes.any(message.message.contains);

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment:
            isMe ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          // Avatar người gửi đến (chỉ hiển thị ở bên trái)
          if (!isMe) ...[
            CircleAvatar(
              radius: 15,
              backgroundColor: isDark ? Colors.grey[800] : Colors.grey[300],
              backgroundImage: otherUserAvatar.isNotEmpty
                  ? CachedNetworkImageProvider(otherUserAvatar, maxWidth: 80)
                  : null,
              child: otherUserAvatar.isEmpty
                  ? Icon(Icons.person,
                      color: isDark ? Colors.white : Colors.black54, size: 16)
                  : null,
            ),
            const SizedBox(width: 8),
          ],

          // Bong bóng tin nhắn
          Flexible(
            child: Column(
              crossAxisAlignment:
                  isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                // Nội dung bubble
                _isVideoShare
                    // Video share card không có padding bubble
                    ? _buildMessageContent(context)
                    : Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: isMe
                              ? const Color(0xFFFE2C55)
                              : (isDark
                                  ? const Color(0xFF242428)
                                  : const Color(0xFFEEEEEE)),
                          borderRadius: BorderRadius.only(
                            topLeft: const Radius.circular(16),
                            topRight: const Radius.circular(16),
                            bottomLeft:
                                Radius.circular(isMe ? 16 : 4),
                            bottomRight:
                                Radius.circular(isMe ? 4 : 16),
                          ),
                        ),
                        child: _buildMessageContent(context),
                      ),

                // Timestamp + Read Receipt (chỉ hiển thị cho tin nhắn của mình)
                if (isMe) _buildReadReceipt(context),

                // Timestamp đơn giản cho tin nhắn người khác
                if (!isMe && message.timestamp > 0)
                  Padding(
                    padding: const EdgeInsets.only(top: 3, left: 2),
                    child: Text(
                      _formatTime(message.timestamp),
                      style: TextStyle(
                        color: isDark ? Colors.white38 : Colors.black38,
                        fontSize: 10,
                      ),
                    ),
                  ),
              ],
            ),
          ),

          if (isMe) const SizedBox(width: 4),
        ],
      ),
    );
  }
}
