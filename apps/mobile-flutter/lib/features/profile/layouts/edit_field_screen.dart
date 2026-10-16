import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/utils/validators.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/profile_provider.dart';

/// Màn hình sửa đổi một trường thông tin riêng biệt (Username, Ngày sinh)
/// Port từ EditActivity.java
class EditFieldScreen extends ConsumerStatefulWidget {
  final String field;

  const EditFieldScreen({
    super.key,
    required this.field,
  });

  @override
  ConsumerState<EditFieldScreen> createState() => _EditFieldScreenState();
}

class _EditFieldScreenState extends ConsumerState<EditFieldScreen> {
  final TextEditingController _controller = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _isSaving = false;
  bool _isModified = false;
  String _errorText = '';

  @override
  void initState() {
    super.initState();
    // Load dữ liệu hiện tại vào ô nhập
    Future.microtask(() async {
      final profile = ref.read(currentUserProfileProvider).valueOrNull;
      if (profile != null) {
        if (widget.field == 'username') {
          _controller.text = profile.username;
        } else if (widget.field == 'birthdate' && profile.birthdate.isNotEmpty) {
          _controller.text = profile.birthdate;
        } else if (widget.field == 'bio') {
          _controller.text = profile.bio;
        }
      }
    });

    _controller.addListener(() {
      setState(() {
        _isModified = true;
      });
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _selectDate(BuildContext context) async {
    final theme = Theme.of(context);
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime(2000),
      firstDate: DateTime(1900),
      lastDate: DateTime.now(),
      builder: (context, child) {
        return Theme(
          data: theme.copyWith(
            colorScheme: theme.colorScheme.copyWith(
              primary: AppTheme.primaryColor,
              onPrimary: Colors.white,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      final day = picked.day.toString().padLeft(2, '0');
      final month = picked.month.toString().padLeft(2, '0');
      final year = picked.year;
      setState(() {
        _controller.text = "$day/$month/$year";
        _errorText = '';
      });
    }
  }

  Future<void> _saveField(String uid) async {
    if (_controller.text.trim().isEmpty) return;

    setState(() {
      _isSaving = true;
      _errorText = '';
    });

    final repo = ref.read(profileRepositoryProvider);
    final value = _controller.text.trim();

    try {
      if (widget.field == 'username') {
        // 1. Kiểm tra định dạng
        final error = Validators.validateUsername(value);
        if (error != null) {
          setState(() {
            _errorText = error;
            _isSaving = false;
          });
          return;
        }

        // 2. Lưu (Repo tự kiểm tra trùng lặp trên Firestore)
        final success = await repo.updateUsername(uid: uid, newUsername: value);
        if (!success) {
          setState(() {
            _errorText = 'Username đã được sử dụng bởi tài khoản khác.';
            _isSaving = false;
          });
          return;
        }
      } else if (widget.field == 'birthdate') {
        // 1. Kiểm tra định dạng ngày sinh dd/mm/yyyy
        final error = Validators.validateBirthdate(value);
        if (error != null) {
          setState(() {
            _errorText = error;
            _isSaving = false;
          });
          return;
        }

        // 2. Lưu
        await repo.updateBirthdate(uid: uid, birthdate: value);
      } else if (widget.field == 'bio') {
        // Lưu tiểu sử
        await repo.updateBio(uid, value);
      }

      // 3. Làm mới auth profile
      ref.invalidate(currentUserProfileProvider);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã cập nhật thông tin thành công!')),
        );
        context.pop();
      }
    } catch (e) {
      setState(() {
        _errorText = 'Lỗi cập nhật: ${e.toString()}';
        _isSaving = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final currentUser = ref.watch(currentUserProvider);
    final isUsername = widget.field == 'username';
    final isBirthdate = widget.field == 'birthdate';
    final isBio = widget.field == 'bio';
    final title = isUsername 
        ? 'Thay đổi Username' 
        : isBirthdate 
            ? 'Thay đổi ngày sinh' 
            : 'Thay đổi tiểu sử';
    final label = isUsername 
        ? 'Username' 
        : isBirthdate 
            ? 'Ngày sinh (DD/MM/YYYY)' 
            : 'Tiểu sử';

    final canSave = _isModified && _controller.text.isNotEmpty && !_isSaving;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      appBar: AppBar(
        backgroundColor: theme.scaffoldBackgroundColor,
        elevation: 0,
        leading: IconButton(
          icon: Icon(Icons.close, color: theme.colorScheme.onSurface),
          onPressed: () => context.pop(),
        ),
        title: Text(
          title,
          style: TextStyle(color: theme.colorScheme.onSurface, fontWeight: FontWeight.bold, fontSize: 17),
        ),
        actions: [
          TextButton(
            onPressed: canSave && currentUser != null ? () => _saveField(currentUser.uid) : null,
            child: Text(
              'Lưu',
              style: TextStyle(
                color: canSave ? theme.colorScheme.primary : theme.colorScheme.onSurface.withValues(alpha: 0.3),
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.8), fontSize: 12),
              ),
              const SizedBox(height: 8),

              // Ô nhập dữ liệu
              TextFormField(
                controller: _controller,
                style: TextStyle(color: theme.colorScheme.onSurface, fontSize: 16),
                keyboardType: isUsername || isBio ? TextInputType.text : TextInputType.datetime,
                maxLength: isBio ? 80 : null,
                decoration: InputDecoration(
                  hintText: isUsername 
                      ? 'Nhập username mới...' 
                      : isBio 
                          ? 'Nhập tiểu sử mới...' 
                          : '01/01/2000',
                  hintStyle: TextStyle(color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.5)),
                  suffixIcon: isBirthdate
                      ? IconButton(
                          icon: Icon(Icons.calendar_today, color: theme.colorScheme.onSurfaceVariant),
                          onPressed: () => _selectDate(context),
                        )
                      : null,
                  focusedBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: theme.colorScheme.primary, width: 2),
                  ),
                  enabledBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: theme.colorScheme.outlineVariant),
                  ),
                  counterStyle: TextStyle(color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.7)),
                ),
              ),
              const SizedBox(height: 12),

              // Báo lỗi nếu có
              if (_errorText.isNotEmpty)
                Text(
                  _errorText,
                  style: TextStyle(color: theme.colorScheme.error, fontSize: 13),
                ),

              const SizedBox(height: 24),

              // Mô tả điều khoản của Username giống TikTok
              if (isUsername)
                Text(
                  'Username có thể chứa các chữ cái, chữ số, dấu gạch dưới, gạch ngang và dấu chấm. Việc thay đổi username sẽ làm thay đổi đường dẫn trang cá nhân của bạn.',
                  style: TextStyle(color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.7), fontSize: 13, height: 1.4),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
