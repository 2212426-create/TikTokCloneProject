import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/constants/app_assets.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../providers/auth_provider.dart';

/// Màn hình Splash Khởi động ViFarm với Debug Log theo Timestamp
class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  // Phase 1: Fade-in
  late Animation<double> _fadeInAnimation;

  // Phase 2: Vertical Jump & Touchdown Squash
  late Animation<double> _translateYAnimation;
  late Animation<double> _scaleYAnimation;
  late Animation<double> _scaleXAnimation;

  // Phase 3: Scale Up & Fade Out Exit
  late Animation<double> _exitScaleAnimation;
  late Animation<double> _exitOpacityAnimation;

  bool _isNavigated = false;
  bool _phase2Completed = false;
  bool _isPrecached = false;
  bool _firstFrameRendered = false;
  String? _initError;

  void _log(String tag, String message) {
    if (kDebugMode) {
      final now = DateTime.now();
      final timestamp =
          '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}:${now.second.toString().padLeft(2, '0')}.${now.millisecond.toString().padLeft(3, '0')}';
      debugPrint('[$tag] $timestamp - $message');
    }
  }

  @override
  void initState() {
    super.initState();
    _log('BOOTSTRAP', 'Starting app bootstrap');
    _log('SPLASH', 'ViFarmLaunchAnimation mounted');

    // Total animation duration 1500ms
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    );

    // 1. Phase 1: Fade-in (0 -> 250ms | 0.0 -> 0.167)
    _fadeInAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.0, 0.167, curve: Curves.easeIn),
      ),
    );

    // 2. Phase 2: Double Jump (250 -> 1050ms | 0.167 -> 0.700)
    _translateYAnimation = TweenSequence<double>([
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.0, end: -22.0)
            .chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 25,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: -22.0, end: 0.0)
            .chain(CurveTween(curve: Curves.easeInCubic)),
        weight: 25,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.0, end: -10.0)
            .chain(CurveTween(curve: Curves.easeOutCubic)),
        weight: 25,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: -10.0, end: 0.0)
            .chain(CurveTween(curve: Curves.easeInCubic)),
        weight: 25,
      ),
    ]).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.167, 0.700),
      ),
    );

    _scaleYAnimation = TweenSequence<double>([
      TweenSequenceItem(tween: ConstantTween(1.0), weight: 40),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 0.95)
            .chain(CurveTween(curve: Curves.easeOut)),
        weight: 5,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.95, end: 1.0)
            .chain(CurveTween(curve: Curves.easeIn)),
        weight: 5,
      ),
      TweenSequenceItem(tween: ConstantTween(1.0), weight: 40),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 0.97)
            .chain(CurveTween(curve: Curves.easeOut)),
        weight: 5,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 0.97, end: 1.0)
            .chain(CurveTween(curve: Curves.easeIn)),
        weight: 5,
      ),
    ]).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.167, 0.700),
      ),
    );

    _scaleXAnimation = TweenSequence<double>([
      TweenSequenceItem(tween: ConstantTween(1.0), weight: 40),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 1.04)
            .chain(CurveTween(curve: Curves.easeOut)),
        weight: 5,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.04, end: 1.0)
            .chain(CurveTween(curve: Curves.easeIn)),
        weight: 5,
      ),
      TweenSequenceItem(tween: ConstantTween(1.0), weight: 40),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.0, end: 1.02)
            .chain(CurveTween(curve: Curves.easeOut)),
        weight: 5,
      ),
      TweenSequenceItem(
        tween: Tween<double>(begin: 1.02, end: 1.0)
            .chain(CurveTween(curve: Curves.easeIn)),
        weight: 5,
      ),
    ]).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.167, 0.700),
      ),
    );

    // 3. Phase 3: Scale Up Zoom & Fade Out Exit (1050 -> 1500ms | 0.700 -> 1.000)
    _exitScaleAnimation = Tween<double>(begin: 1.0, end: 2.8).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.700, 1.000, curve: Curves.easeInOutCubic),
      ),
    );

    _exitOpacityAnimation = Tween<double>(begin: 1.0, end: 0.0).animate(
      CurvedAnimation(
        parent: _controller,
        curve: const Interval(0.700, 1.000, curve: Curves.easeIn),
      ),
    );

    // Track animation stages
    _controller.addListener(() {
      if (!_firstFrameRendered && _controller.value > 0) {
        _firstFrameRendered = true;
        _log('SPLASH', 'First animation frame rendered');
      }

      if (_controller.value >= 0.167 && _controller.value < 0.20) {
        _log('SPLASH', 'Phase 2 double jump started');
      }

      if (_controller.value >= 0.700 && !_phase2Completed) {
        _phase2Completed = true;
        _log('SPLASH', 'Phase 2 double jump completed');
        _checkAndProceedPhase3();
      }
    });

    _controller.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _log('SPLASH', 'Phase 3 exit zoom completed');
        _navigateHome();
      }
    });

    // Capture first frame callback
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _log('SPLASH', 'Post frame callback executed');
      }
    });

    _controller.forward();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_isPrecached) {
      _isPrecached = true;
      precacheImage(const AssetImage(AppAssets.logo), context);
      _log('SPLASH', 'Logo asset precached');
    }
  }

  @override
  void dispose() {
    _log('NAV', 'Disposing SplashScreen');
    _controller.dispose();
    super.dispose();
  }

  void _checkAndProceedPhase3() {
    if (_isNavigated || !mounted) return;

    final authState = ref.read(authStateProvider);

    authState.when(
      data: (user) {
        _log('BOOTSTRAP', 'App bootstrap ready (User: ${user != null ? "logged_in" : "guest"})');
        if (_controller.value >= 0.700 && !_controller.isAnimating) {
          _log('SPLASH', 'Phase 3 exit zoom started');
          _controller.forward(from: 0.700);
        }
      },
      loading: () {
        _log('BOOTSTRAP', 'Waiting for auth/bootstrap initialization...');
        if (_controller.value >= 0.700 && _controller.isAnimating) {
          _controller.stop();
        }
      },
      error: (err, _) {
        _log('BOOTSTRAP', 'Bootstrap error: $err');
        setState(() {
          _initError = 'Lỗi kết nối: ${err.toString()}';
        });
      },
    );
  }

  void _navigateHome() {
    if (_isNavigated || !mounted) return;
    _isNavigated = true;
    _log('NAV', 'Navigating to target route (/home) and disposing splash');
    context.go('/home');
  }

  void _retryInit() {
    _log('BOOTSTRAP', 'Retrying initialization');
    setState(() {
      _initError = null;
    });
    ref.invalidate(authStateProvider);
    _controller.forward(from: 0.700);
  }

  @override
  Widget build(BuildContext context) {
    final disableAnimations = MediaQuery.of(context).disableAnimations;

    ref.listen(authStateProvider, (previous, next) {
      if (!next.isLoading && _phase2Completed && !_isNavigated) {
        if (_initError == null) {
          _log('BOOTSTRAP', 'Auth state ready via listener');
          if (!_controller.isAnimating) {
            _log('SPLASH', 'Phase 3 exit zoom started');
            _controller.forward(from: _controller.value < 0.700 ? 0.700 : _controller.value);
          }
        }
      }
    });

    final isDark = Theme.of(context).brightness == Brightness.dark;

    return AbsorbPointer(
      absorbing: !_isNavigated && _initError == null,
      child: Scaffold(
        backgroundColor: isDark ? AppColors.darkBg : AppColors.lightBg,
        body: Center(
          child: disableAnimations
              ? _buildStaticFallback(isDark)
              : AnimatedBuilder(
                  animation: _controller,
                  builder: (context, child) {
                    return Opacity(
                      opacity: _exitOpacityAnimation.value,
                      child: Transform.scale(
                        scale: _exitScaleAnimation.value,
                        child: Opacity(
                          opacity: _fadeInAnimation.value,
                          child: Transform.translate(
                            offset: Offset(0, _translateYAnimation.value),
                            child: Transform.scale(
                              scaleX: _scaleXAnimation.value,
                              scaleY: _scaleYAnimation.value,
                              child: _buildLogoWidget(isDark),
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
        ),
      ),
    );
  }

  Widget _buildLogoWidget(bool isDark) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // ViFarm Logo
        Container(
          width: 120,
          height: 120,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(24),
            boxShadow: [
              BoxShadow(
                color: AppColors.primary.withAlpha(50),
                blurRadius: 30,
                spreadRadius: 4,
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(24),
            child: Image.asset(
              AppAssets.logo,
              fit: BoxFit.contain,
              errorBuilder: (context, error, stackTrace) =>
                  const Icon(Icons.grass, size: 72, color: AppColors.primary),
            ),
          ),
        ),
        const SizedBox(height: 20),

        // ViFarm Name
        Text(
          AppConstants.appName,
          style: TextStyle(
            fontSize: 30,
            fontWeight: FontWeight.bold,
            color: isDark ? AppColors.darkTextPrimary : AppColors.lightTextPrimary,
            letterSpacing: 2,
          ),
        ),
        const SizedBox(height: 4),

        Text(
          'Video Kỹ Thuật & Mạng Xã Hội Nông Nghiệp',
          style: TextStyle(
            fontSize: 12,
            color: isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary,
          ),
        ),

        if (_initError != null) ...[
          const SizedBox(height: 24),
          Text(
            _initError!,
            style: const TextStyle(color: Colors.redAccent, fontSize: 12),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 12),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
            ),
            onPressed: _retryInit,
            icon: const Icon(Icons.refresh, size: 16),
            label: const Text('Thử lại', style: TextStyle(fontSize: 12)),
          ),
        ],

        if (_phase2Completed && !_isNavigated && _initError == null && ref.watch(authStateProvider).isLoading) ...[
          const SizedBox(height: 32),
          const SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: AppColors.primary,
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildStaticFallback(bool isDark) {
    return _buildLogoWidget(isDark);
  }
}
