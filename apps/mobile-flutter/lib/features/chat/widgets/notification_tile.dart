import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../models/notification_model.dart';

/// Widget hiển thị một dòng thông báo (Like, Comment, Follow, Chat, Warning)
/// Port từ NotificationAdapter.java và InboxActivity.java
class NotificationTile extends StatelessWidget {
  final NotificationModel notification;

  const NotificationTile({
    super.key,
    required this.notification,
  });

  /// Tính toán thời gian tương đối giống Java
  String _formatTime(int timestamp) {
    if (timestamp <= 0) return '';
    final difference = DateTime.now().millisecondsSinceEpoch - timestamp;
    final diffMinutes = difference ~/ (1000 * 60);
    final diffHours = difference ~/ (1000 * 60 * 60);
    final diffDays = difference ~/ (1000 * 60 * 60 * 24);

    if (diffMinutes <= 60) {
      return '${diffMinutes}m';
    } else if (diffHours <= 24) {
      return '${diffHours}h';
    } else {
      return '${diffDays}d';
    }
  }

  /// Trả về chuỗi mô tả hành động bằng tiếng Việt
  String _getActionText(String action) {
    if (action.startsWith('APPEAL_REQUEST')) {
      final parts = action.split('|');
      final reason = parts.length > 2 ? parts[2] : 'Vi phạm tiêu chuẩn cộng đồng';
      return 'Video bị tố cáo vì: $reason. Bấm vào đây để kháng cáo.';
    }

    switch (action) {
      case AppConstants.actionFollow:
        return 'đã bắt đầu theo dõi bạn.';
      case AppConstants.actionComment:
        return 'đã bình luận về video của bạn.';
      case AppConstants.actionLike:
        return 'đã thích video của bạn.';
      case AppConstants.actionChat:
        return 'đã gửi tin nhắn cho bạn.';
      default:
        return '';
    }
  }

  /// Lấy Icon & màu tương ứng cho loại thông báo
  Widget _getNotificationIcon(String action) {
    IconData iconData;
    Color iconColor;
    Color bgColor;

    if (action.startsWith('APPEAL_REQUEST')) {
      iconData = Icons.warning_amber_rounded;
      iconColor = Colors.orange;
      bgColor = Colors.orange.withValues(alpha: 0.1);
    } else {
      switch (action) {
        case AppConstants.actionFollow:
          iconData = Icons.person_add_rounded;
          iconColor = Colors.blue;
          bgColor = Colors.blue.withValues(alpha: 0.1);
          break;
        case AppConstants.actionComment:
          iconData = Icons.chat_bubble_rounded;
          iconColor = Colors.green;
          bgColor = Colors.green.withValues(alpha: 0.1);
          break;
        case AppConstants.actionLike:
          iconData = Icons.favorite_rounded;
          iconColor = const Color(0xFFFE2C55); // TikTok Red/Pink
          bgColor = const Color(0xFFFE2C55).withValues(alpha: 0.1);
          break;
        case AppConstants.actionChat:
          iconData = Icons.mail_rounded;
          iconColor = Colors.purple;
          bgColor = Colors.purple.withValues(alpha: 0.1);
          break;
        default:
          iconData = Icons.notifications_rounded;
          iconColor = Colors.grey;
          bgColor = Colors.grey.withValues(alpha: 0.1);
      }
    }

    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: bgColor,
        shape: BoxShape.circle,
      ),
      child: Icon(iconData, color: iconColor, size: 22),
    );
  }

  /// Hiển thị Dialog kháng cáo gửi lên Firestore collection "reports"
  /// Port từ InboxActivity.showAppealDialog()
  void _showAppealDialog(BuildContext context, String reportId) {
    final docRef = FirebaseFirestore.instance.collection(AppConstants.reportsCollection).doc(reportId);

    docRef.get().then((doc) {
      if (!context.mounted) return;
      if (!doc.exists) return;
      final currentAppeal = doc.data()?['appeal'] as String?;
      if (currentAppeal != null && currentAppeal.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Bạn đã gửi kháng cáo cho báo cáo này rồi.')),
        );
        return;
      }

      final controller = TextEditingController();

      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          backgroundColor: const Color(0xFF1E1E1E),
          title: const Text(
            'Kháng cáo vi phạm',
            style: TextStyle(color: Colors.white),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Vui lòng giải thích để quản trị viên xem xét lại.',
                style: TextStyle(color: AppTheme.textSecondary, fontSize: 14),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: controller,
                maxLines: 3,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(
                  hintText: 'Nhập lý do kháng cáo của bạn...',
                  hintStyle: TextStyle(color: AppTheme.textHint),
                  enabledBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: AppTheme.dividerColor),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderSide: BorderSide(color: Color(0xFFFE2C55)),
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
              ),
              onPressed: () {
                final appealText = controller.text.trim();
                if (appealText.isNotEmpty) {
                  docRef.update({
                    'appeal': appealText,
                    'status': 'pending',
                  }).then((_) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Đã gửi kháng cáo!')),
                      );
                      Navigator.pop(context);
                    }
                  }).catchError((_) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Lỗi khi gửi kháng cáo')),
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
    });
  }

  @override
  Widget build(BuildContext context) {
    final isAppeal = notification.action.startsWith('APPEAL_REQUEST');

    return InkWell(
      onTap: () {
        if (isAppeal) {
          final parts = notification.action.split('|');
          if (parts.length > 1) {
            _showAppealDialog(context, parts[1]);
          }
        } else if (notification.action == AppConstants.actionLike &&
            notification.videoId != null &&
            notification.videoId!.isNotEmpty) {
          // Khi ai đó tim video -> Mở video đó
          context.push('/video/${notification.videoId}');
        } else if (notification.action == AppConstants.actionComment &&
            notification.videoId != null &&
            notification.videoId!.isNotEmpty) {
          // Khi ai đó bình luận video -> Mở video đó và tự động hiển thị comment sheet để reply
          context.push('/video/${notification.videoId}?showComments=true');
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(color: AppTheme.dividerColor, width: 0.5),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _getNotificationIcon(notification.action),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '@${notification.fromUsername}',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  RichText(
                    text: TextSpan(
                      style: const TextStyle(
                        color: AppTheme.textSecondary,
                        fontSize: 14,
                      ),
                      children: [
                        TextSpan(
                          text: _getActionText(notification.action),
                          style: TextStyle(
                            color: isAppeal ? Colors.orange : AppTheme.textSecondary,
                          ),
                        ),
                        const TextSpan(text: '  '),
                        TextSpan(
                          text: _formatTime(notification.timestamp),
                          style: const TextStyle(
                            color: AppTheme.textHint,
                            fontSize: 12,
                          ),
                        ),
                      ],
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
}
