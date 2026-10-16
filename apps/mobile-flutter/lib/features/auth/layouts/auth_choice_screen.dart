import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_strings.dart';
import '../../../core/theme/app_theme.dart';
import '../providers/auth_provider.dart';

/// Màn hình chọn phương thức đăng nhập/đăng ký ViFarm
class AuthChoiceScreen extends ConsumerWidget {
  const AuthChoiceScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authNotifierProvider);

    // Lắng nghe và hiển thị lỗi nếu có
    ref.listen<AuthState>(authNotifierProvider, (prev, next) {
      if (next.errorMessage != null &&
          next.errorMessage != prev?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white, size: 18),
                const SizedBox(width: 8),
                Expanded(child: Text(next.errorMessage!)),
              ],
            ),
            backgroundColor: AppTheme.errorColor,
          ),
        );
        ref.read(authNotifierProvider.notifier).clearError();
      }
    });

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Stack(
          children: [
            // Background gradient overlay
            Positioned.fill(
              child: DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      AppTheme.primaryColor.withValues(alpha: 0.03),
                      AppTheme.backgroundColor,
                    ],
                    stops: const [0.0, 0.6, 1.0],
                  ),
                ),
              ),
            ),

            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 32),
              child: Column(
                children: [
                  const Spacer(flex: 2),

                  // Logo với glow effect
                  Container(
                    width: 90,
                    height: 90,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [
                        BoxShadow(
                          color: AppTheme.primaryColor.withValues(alpha: 0.4),
                          blurRadius: 30,
                          spreadRadius: 2,
                        ),
                        BoxShadow(
                          color: AppTheme.secondaryColor.withValues(alpha: 0.2),
                          blurRadius: 40,
                          spreadRadius: 1,
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(24),
                      child: Image.asset(AppAssets.logo, fit: BoxFit.contain),
                    ),
                  ),
                  const SizedBox(height: 28),

                  // Title
                  const Text(
                    'Đăng nhập vào ViFarm',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.w800,
                      color: AppTheme.textPrimary,
                      letterSpacing: 0.3,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    'Khám phá kỹ thuật nông nghiệp, trao đổi mua bán\nvật tư nông sản & kết nối cộng đồng nhà vườn.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.white.withValues(alpha: 0.55),
                      height: 1.6,
                    ),
                  ),

                  const SizedBox(height: 48),

                  // Đăng nhập bằng Google
                  _AuthChoiceButton(
                    icon: const _GoogleLogo(),
                    label: AppStrings.btnChoiceGoogle,
                    onTap: authState.isLoading
                        ? () {}
                        : () async {
                            final success = await ref
                                .read(authNotifierProvider.notifier)
                                .signInWithGoogle();
                            if (success && context.mounted) {
                              context.go('/home');
                            }
                          },
                  ),

                  const SizedBox(height: 14),

                  // Đăng nhập bằng Email & Mật khẩu
                  _AuthChoiceButton(
                    icon: const Icon(
                      Icons.email_outlined,
                      color: AppTheme.textPrimary,
                      size: 22,
                    ),
                    label: 'Sử dụng Email & Mật khẩu',
                    onTap: authState.isLoading
                        ? () {}
                        : () => context.push('/auth/login'),
                  ),

                  const Spacer(flex: 3),

                  // Bottom link
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        AppStrings.signInAlt.replaceAll('Sign up.', ''),
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.5),
                          fontSize: 14,
                        ),
                      ),
                      GestureDetector(
                        onTap: () => context.push('/auth/signup'),
                        child: const Text(
                          AppStrings.signUp,
                          style: TextStyle(
                            color: AppTheme.primaryColor,
                            fontSize: 14,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),
                ],
              ),
            ),

            // Loading overlay
            if (authState.isLoading)
              Container(
                color: Colors.black54,
                child: const Center(
                  child: CircularProgressIndicator(
                    color: AppTheme.primaryColor,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _GoogleLogo extends StatelessWidget {
  const _GoogleLogo();

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      'assets/images/google_logo.svg',
      width: 22,
      height: 22,
      semanticsLabel: 'Google',
    );
  }
}

class _AuthChoiceButton extends StatelessWidget {
  final Widget icon;
  final String label;
  final VoidCallback onTap;

  const _AuthChoiceButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        splashColor: AppTheme.primaryColor.withValues(alpha: 0.1),
        highlightColor: AppTheme.primaryColor.withValues(alpha: 0.05),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(vertical: 15),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.white.withValues(alpha: 0.12)),
            borderRadius: BorderRadius.circular(12),
            color: Colors.white.withValues(alpha: 0.04),
          ),
          child: Row(
            children: [
              const SizedBox(width: 18),
              icon,
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(
                    color: AppTheme.textPrimary,
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    letterSpacing: 0.2,
                  ),
                ),
              ),
              Icon(
                Icons.arrow_forward_ios,
                color: Colors.white.withValues(alpha: 0.2),
                size: 14,
              ),
              const SizedBox(width: 16),
            ],
          ),
        ),
      ),
    );
  }
}
