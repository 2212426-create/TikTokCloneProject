// ignore_for_file: deprecated_member_use
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import '../../../core/theme/app_theme.dart';

/// Màn hình Camera cho phép chọn quay video hoặc tải lên từ thư viện máy
class CameraScreen extends StatefulWidget {
  const CameraScreen({super.key});

  @override
  State<CameraScreen> createState() => _CameraScreenState();
}

class _CameraScreenState extends State<CameraScreen> {
  final ImagePicker _picker = ImagePicker();
  bool _isLoading = false;

  /// Xử lý chọn/ghi hình video bằng image_picker
  Future<void> _pickVideo(ImageSource source) async {
    setState(() {
      _isLoading = true;
    });

    try {
      final XFile? file = await _picker.pickVideo(
        source: source,
        maxDuration: const Duration(minutes: 5), // Tối đa 5 phút
      );

      if (file == null) {
        setState(() {
          _isLoading = false;
        });
        return;
      }

      // Xác minh kích thước file (< 100MB)
      final sizeInBytes = await file.length();
      final sizeInMb = sizeInBytes / (1024 * 1024);

      if (sizeInMb > 100) {
        if (mounted) {
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              backgroundColor: const Color(0xFF1D1D1F),
              title: const Text(
                'Lỗi kích thước',
                style: TextStyle(color: Colors.white),
              ),
              content: const Text(
                'Dung lượng video vượt quá 100MB. Vui lòng chọn tệp nhỏ hơn.',
                style: TextStyle(color: Colors.grey),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text(
                    'OK',
                    style: TextStyle(color: AppTheme.primaryColor),
                  ),
                ),
              ],
            ),
          );
        }
        setState(() {
          _isLoading = false;
        });
        return;
      }

      // Hợp lệ -> Chuyển hướng sang màn hình thêm mô tả video
      if (mounted) {
        context.push('/video/description', extra: file);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Đã xảy ra lỗi khi chọn video: $e')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: Colors.white, size: 28),
          onPressed: () => context.pop(),
        ),
        title: const Text(
          'Đăng video',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: AppTheme.primaryColor),
            )
          : Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.video_library_outlined,
                    size: 80,
                    color: Colors.white30,
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    'Tạo nội dung mới của bạn',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 16,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 48),

                  if (!kIsWeb) ...[
                    _buildUploadCard(
                      icon: Icons.videocam_rounded,
                      title: 'Quay video mới',
                      subtitle: 'Ghi hình tối đa 5 phút bằng máy ảnh',
                      onTap: () => _pickVideo(ImageSource.camera),
                    ),
                    const SizedBox(height: 20),
                  ],

                  // Nút tải lên từ thư viện
                  _buildUploadCard(
                    icon: Icons.photo_library_rounded,
                    title: 'Chọn từ thư viện',
                    subtitle: 'Chọn tệp video có sẵn trong máy (< 100MB)',
                    onTap: () => _pickVideo(ImageSource.gallery),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildUploadCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 24.0),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.12),
              width: 1,
            ),
          ),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: const BoxDecoration(
                  color: Colors.black26,
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: AppTheme.primaryColor, size: 32),
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      style: TextStyle(color: Colors.grey[500], fontSize: 12),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.white30),
            ],
          ),
        ),
      ),
    );
  }
}
