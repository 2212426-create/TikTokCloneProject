import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/profile_provider.dart';

/// Màn hình Chỉnh sửa hồ sơ cá nhân
/// Port từ EditProfileActivity.java
class EditProfileScreen extends ConsumerStatefulWidget {
  const EditProfileScreen({super.key});

  @override
  ConsumerState<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends ConsumerState<EditProfileScreen> {
  bool _isSavingAvatar = false;

  Future<void> _pickAndUploadAvatar(String uid) async {
    final picker = ImagePicker();
    try {
      final pickedFile = await picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 85, // Giới hạn kích thước ảnh
      );

      if (pickedFile != null) {
        setState(() {
          _isSavingAvatar = true;
        });

        // 1. Tải lên Firebase Storage và cập nhật Firestore
        await ref.read(profileRepositoryProvider).updateAvatar(uid, pickedFile);

        // 2. Làm mới profile
        ref.invalidate(currentUserProfileProvider);

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Đã cập nhật ảnh đại diện thành công!'),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Lỗi tải ảnh: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSavingAvatar = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final currentUser = ref.watch(currentUserProvider);
    final profileState = ref.watch(currentUserProfileProvider);

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: theme.scaffoldBackgroundColor,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: theme.colorScheme.onSurface),
          onPressed: () => context.pop(),
        ),
        title: Text(
          'Sửa hồ sơ',
          style: TextStyle(
            color: theme.colorScheme.onSurface,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
      ),
      body: profileState.when(
        data: (profile) {
          if (profile == null || currentUser == null) {
            return Center(
              child: Text(
                'Không tìm thấy thông tin đăng nhập',
                style: TextStyle(color: theme.colorScheme.onSurfaceVariant),
              ),
            );
          }

          return SingleChildScrollView(
            child: Column(
              children: [
                const SizedBox(height: 24),

                // 1. Phần chọn Ảnh đại diện
                Center(
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      CircleAvatar(
                        radius: 54,
                        backgroundColor: isDark
                            ? const Color(0xFF1E1E22)
                            : const Color(0xFFF0F0F2),
                        backgroundImage: profile.avatarUrl.isNotEmpty
                            ? CachedNetworkImageProvider(
                                profile.avatarUrl,
                                maxWidth: 120,
                              )
                            : const AssetImage(
                                    'assets/images/default_avatar.png',
                                  )
                                  as ImageProvider,
                        child: profile.avatarUrl.isEmpty
                            ? Icon(
                                Icons.person,
                                size: 54,
                                color: theme.colorScheme.onSurfaceVariant,
                              )
                            : null,
                      ),
                      // Lớp đen mờ & icon camera
                      GestureDetector(
                        onTap: _isSavingAvatar
                            ? null
                            : () => _pickAndUploadAvatar(currentUser.uid),
                        child: Container(
                          width: 108,
                          height: 108,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: Colors.black.withValues(alpha: 0.35),
                            border: Border.all(
                              color: Colors.white30,
                              width: 1.5,
                            ),
                          ),
                          child: const Icon(
                            Icons.camera_alt,
                            color: Colors.white,
                            size: 30,
                          ),
                        ),
                      ),
                      if (_isSavingAvatar)
                        const SizedBox(
                          width: 108,
                          height: 108,
                          child: CircularProgressIndicator(
                            valueColor: AlwaysStoppedAnimation<Color>(
                              AppTheme.primaryColor,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                TextButton(
                  onPressed: _isSavingAvatar
                      ? null
                      : () => _pickAndUploadAvatar(currentUser.uid),
                  child: Text(
                    'Thay đổi ảnh',
                    style: TextStyle(
                      color: theme.colorScheme.primary,
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(height: 24),

                // 2. Danh sách các trường thông tin
                Divider(color: theme.colorScheme.outlineVariant, height: 1),

                // Thay đổi Username
                _buildEditRow(
                  label: 'Username',
                  value: '@${profile.username}',
                  icon: Icons.alternate_email,
                  onTap: () {
                    context.push('/profile/edit/username');
                  },
                ),

                // Thay đổi Bio
                _buildEditRow(
                  label: 'Tiểu sử',
                  value: profile.bio.isNotEmpty
                      ? profile.bio
                      : 'Chưa thiết lập',
                  icon: Icons.info_outline,
                  onTap: () {
                    context.push('/profile/edit/bio');
                  },
                ),

                // Thay đổi Ngày sinh
                _buildEditRow(
                  label: 'Ngày sinh',
                  value: profile.birthdate.isNotEmpty
                      ? profile.birthdate
                      : 'Chưa cập nhật',
                  icon: Icons.calendar_today_outlined,
                  onTap: () {
                    context.push('/profile/edit/birthdate');
                  },
                ),

                Divider(color: theme.colorScheme.outlineVariant, height: 1),
              ],
            ),
          );
        },
        loading: () => const Center(
          child: CircularProgressIndicator(
            valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
          ),
        ),
        error: (error, _) => Center(
          child: Text(
            'Lỗi: ${error.toString()}',
            style: const TextStyle(color: Colors.redAccent),
          ),
        ),
      ),
    );
  }

  Widget _buildEditRow({
    required String label,
    required String value,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    final theme = Theme.of(context);

    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
        child: Row(
          children: [
            Icon(
              icon,
              color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.7),
              size: 20,
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 3,
              child: Text(
                label,
                style: TextStyle(
                  color: theme.colorScheme.onSurface,
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            Expanded(
              flex: 7,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Expanded(
                    child: Text(
                      value,
                      style: TextStyle(
                        color: theme.colorScheme.onSurfaceVariant,
                        fontSize: 14,
                      ),
                      textAlign: TextAlign.end,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.arrow_forward_ios,
                    color: theme.colorScheme.onSurfaceVariant.withValues(
                      alpha: 0.4,
                    ),
                    size: 14,
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
