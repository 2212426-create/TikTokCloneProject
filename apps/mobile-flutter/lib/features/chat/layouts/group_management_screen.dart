import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../../../core/theme/app_theme.dart';
import '../../video_upload/providers/upload_provider.dart';
import '../widgets/member_management_sheet.dart';

class GroupManagementScreen extends ConsumerWidget {
  final String groupId;

  const GroupManagementScreen({super.key, required this.groupId});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return StreamBuilder<DocumentSnapshot>(
      stream: FirebaseFirestore.instance.collection('chats').doc(groupId).snapshots(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const Scaffold(body: Center(child: CircularProgressIndicator()));
        
        final data = snapshot.data!.data() as Map<String, dynamic>;
        final approvalRequired = data['adminApprovalRequired'] ?? false;
        final inviteLinkEnabled = data['allowInviteViaLink'] ?? true;
        final memberIds = List<String>.from(data['memberIds'] ?? []);
        final groupName = data['groupName'] ?? 'Nhóm';
        final avatarUrl = data['avatarUrl'] ?? '';

        return Scaffold(
          appBar: AppBar(
            title: const Text('Quản lý nhóm', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => Navigator.pop(context)),
          ),
          body: ListView(
            padding: const EdgeInsets.symmetric(vertical: 10),
            children: [
              _buildHeader(context, ref, groupName, avatarUrl),
              const SizedBox(height: 20),
              _buildSwitchTile(
                'Cần quản trị viên phê duyệt để tham gia', 
                approvalRequired, 
                (v) => FirebaseFirestore.instance.collection('chats').doc(groupId).update({'adminApprovalRequired': v})
              ),
              const SizedBox(height: 10),
              _buildSwitchTile(
                'Cho phép mời qua liên kết', 
                inviteLinkEnabled, 
                (v) => FirebaseFirestore.instance.collection('chats').doc(groupId).update({'allowInviteViaLink': v}),
                subtitle: 'Cho phép người khác tham gia bằng liên kết mời',
              ),
              const SizedBox(height: 20),
              _buildNavigationTile('Quản lý quản trị viên (0/5)', () {}),
              _buildNavigationTile('Xóa thành viên', () {
                showModalBottomSheet(
                  context: context,
                  isScrollControlled: true,
                  backgroundColor: Colors.transparent,
                  builder: (context) => MemberManagementSheet(groupId: groupId, memberIds: memberIds),
                );
              }),
              const SizedBox(height: 20),
              _buildNavigationTile('Chuyển quyền sở hữu', () {}),
              const SizedBox(height: 20),
              ListTile(
                title: const Center(child: Text('Kết thúc nhóm', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold))),
                onTap: () => _endGroup(context),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSwitchTile(String title, bool value, Function(bool) onChanged, {String? subtitle}) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [BoxShadow(color: Colors.grey.withValues(alpha: 0.05), blurRadius: 4, spreadRadius: 1)],
      ),
      child: ListTile(
        title: Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
        subtitle: subtitle != null ? Text(subtitle, style: const TextStyle(fontSize: 12, color: Colors.grey)) : null,
        trailing: Switch(value: value, onChanged: onChanged, activeColor: AppTheme.primaryColor),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, WidgetRef ref, String name, String avatar) {
    return Column(
      children: [
        GestureDetector(
          onTap: () => _updateGroupAvatar(context, ref),
          child: Stack(
            children: [
              CircleAvatar(
                radius: 40,
                backgroundImage: avatar.isNotEmpty ? NetworkImage(avatar) : null,
                child: avatar.isEmpty ? const Icon(Icons.group, size: 40) : null,
              ),
              Positioned(
                right: 0,
                bottom: 0,
                child: CircleAvatar(
                  radius: 12,
                  backgroundColor: AppTheme.primaryColor,
                  child: const Icon(Icons.camera_alt, size: 14, color: Colors.white),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        InkWell(
          onTap: () => _editGroupName(context, name),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(width: 8),
              const Icon(Icons.edit, size: 16, color: Colors.grey),
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _updateGroupAvatar(BuildContext context, WidgetRef ref) async {
    final picker = ImagePicker();
    final picked = await picker.pickImage(source: ImageSource.gallery, imageQuality: 50);
    if (picked != null) {
      try {
        final uploadRepo = ref.read(uploadRepositoryProvider);
        final url = await uploadRepo.uploadImageToCloudinary(file: picked);
        
        await FirebaseFirestore.instance.collection('chats').doc(groupId).update({
          'avatarUrl': url,
        });

        _sendSystemMessage('Đã cập nhật ảnh đại diện nhóm.');
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Lỗi cập nhật ảnh: $e')));
        }
      }
    }
  }

  Future<void> _editGroupName(BuildContext context, String currentName) async {
    final controller = TextEditingController(text: currentName);
    final newName = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Đổi tên nhóm'),
        content: TextField(controller: controller, autofocus: true, decoration: const InputDecoration(hintText: 'Nhập tên nhóm mới')),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Hủy')),
          TextButton(onPressed: () => Navigator.pop(ctx, controller.text.trim()), child: const Text('Lưu')),
        ],
      ),
    );

    if (newName != null && newName.isNotEmpty && newName != currentName) {
      await FirebaseFirestore.instance.collection('chats').doc(groupId).update({'groupName': newName});
      _sendSystemMessage('Đã đổi tên nhóm thành "$newName".');
    }
  }

  void _sendSystemMessage(String message) {
    FirebaseFirestore.instance
        .collection('chats')
        .doc(groupId)
        .collection('messages')
        .add({
      'senderId': 'system',
      'message': message,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
      'type': 'system',
    });
    
    FirebaseFirestore.instance.collection('chats').doc(groupId).update({
      'lastMessage': message,
      'lastMessageAt': FieldValue.serverTimestamp(),
    });
  }

  Widget _buildNavigationTile(String title, VoidCallback onTap) {
    return ListTile(
      title: Text(title, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500)),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }

  Future<void> _endGroup(BuildContext context) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Kết thúc nhóm?'),
        content: const Text('Hành động này sẽ xóa vĩnh viễn nhóm chat này cho tất cả mọi người.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Hủy')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Kết thúc', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirm == true) {
      await FirebaseFirestore.instance.collection('chats').doc(groupId).delete();
      if (context.mounted) {
        Navigator.pop(context); // Close management
        Navigator.pop(context); // Close settings
        // Should ideally navigate back to inbox via router
      }
    }
  }
}
