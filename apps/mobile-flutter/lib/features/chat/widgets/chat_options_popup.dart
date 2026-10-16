import 'package:flutter/material.dart';
import '../models/chat_preview_model.dart';

/// Hiển thị Pop-up Menu các tùy chọn cho một cuộc hội thoại
/// Sử dụng showMenu để hiển thị tại vị trí người dùng nhấn giữ
Future<void> showChatOptionsPopup(
  BuildContext context, {
  required Offset tapPosition,
  required ChatPreview chat,
  required VoidCallback onMarkUnread,
  required VoidCallback onArchive,
  required VoidCallback onTogglePin,
  required VoidCallback onToggleMute,
  required VoidCallback onToggleStar,
  required VoidCallback onDelete,
  required VoidCallback onBlock,
  required Function(ChatPreview) onCreateGroup,
}) async {
  final RenderBox overlay = Overlay.of(context).context.findRenderObject() as RenderBox;
  
  // Tính toán vị trí hiển thị menu
  final RelativeRect position = RelativeRect.fromRect(
    Rect.fromLTWH(tapPosition.dx, tapPosition.dy, 0, 0),
    Offset.zero & overlay.size,
  );

  final result = await showMenu<String>(
    context: context,
    position: position,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    elevation: 8,
    items: [
      PopupMenuItem(
        value: 'markRead',
        child: _buildMenuItem(
          icon: chat.isRead ? Icons.mark_chat_unread_outlined : Icons.mark_chat_read_outlined,
          text: chat.isRead ? 'Đánh dấu là chưa đọc' : 'Đánh dấu là đã đọc',
        ),
      ),
      PopupMenuItem(
        value: 'archive',
        child: _buildMenuItem(
          icon: Icons.archive_outlined,
          text: 'Lưu trữ',
        ),
      ),
      PopupMenuItem(
        value: 'pin',
        child: _buildMenuItem(
          icon: chat.isPinned ? Icons.push_pin : Icons.push_pin_outlined,
          text: chat.isPinned ? 'Bỏ ghim' : 'Ghim lên đầu',
        ),
      ),
      PopupMenuItem(
        value: 'mute',
        child: _buildMenuItem(
          icon: chat.isMuted ? Icons.notifications_off : Icons.notifications_active_outlined,
          text: chat.isMuted ? 'Tắt tiếng' : 'Bật thông báo',
        ),
      ),
      PopupMenuItem(
        value: 'star',
        child: _buildMenuItem(
          icon: chat.isStarred ? Icons.star : Icons.star_border,
          text: chat.isStarred ? 'Bỏ gắn dấu sao' : 'Gắn dấu sao',
        ),
      ),
      const PopupMenuDivider(),
      if (chat.isGroup)
        PopupMenuItem(
          value: 'leaveGroup',
          child: _buildMenuItem(
            icon: Icons.logout,
            text: 'Rời khỏi nhóm',
            color: Colors.red,
          ),
        )
      else
        PopupMenuItem(
          value: 'createGroup',
          child: _buildMenuItem(
            icon: Icons.group_add_outlined,
            text: 'Tạo nhóm với người này',
          ),
        ),
      PopupMenuItem(
        value: 'delete',
        child: _buildMenuItem(
          icon: Icons.delete_outline,
          text: 'Xóa',
          color: Colors.red,
        ),
      ),
      PopupMenuItem(
        value: 'block',
        child: _buildMenuItem(
          icon: Icons.block,
          text: 'Chặn',
          color: Colors.red,
        ),
      ),
    ],
  );

  if (result == null) return;

  switch (result) {
    case 'markRead':
      onMarkUnread();
      break;
    case 'archive':
      onArchive();
      break;
    case 'pin':
      onTogglePin();
      break;
    case 'mute':
      onToggleMute();
      break;
    case 'star':
      onToggleStar();
      break;
    case 'createGroup':
      onCreateGroup(chat);
      break;
    case 'leaveGroup':
      onDelete(); // Tạm dùng logic xóa/rời nhóm
      break;
    case 'block':
      onBlock();
      break;
    case 'delete':
      _showDeleteConfirmation(context, onDelete);
      break;
  }
}

void _showDeleteConfirmation(BuildContext context, VoidCallback onConfirm) {
  showDialog(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Xóa cuộc trò chuyện?'),
      content: const Text('Tất cả tin nhắn sẽ bị xóa và không thể khôi phục.'),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Hủy'),
        ),
        TextButton(
          onPressed: () {
            Navigator.pop(context);
            onConfirm();
          },
          child: const Text('Xóa', style: TextStyle(color: Colors.red)),
        ),
      ],
    ),
  );
}

Widget _buildMenuItem({
  required IconData icon,
  required String text,
  Color? color,
}) {
  return Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, color: color ?? Colors.black87, size: 24),
        const SizedBox(width: 16),
        Text(
          text,
          style: TextStyle(
            color: color ?? Colors.black87,
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    ),
  );
}
