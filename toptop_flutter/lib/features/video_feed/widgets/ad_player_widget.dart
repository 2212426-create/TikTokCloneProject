import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:video_player/video_player.dart';
import 'package:visibility_detector/visibility_detector.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

import '../../auth/providers/auth_provider.dart';

import '../models/ad_model.dart';
import '../providers/video_provider.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/constants/app_constants.dart';

/// Widget phát Video Quảng cáo được tài trợ (Sponsored Ads)
/// Tự động phát/dừng dựa trên độ hiển thị và có thanh CTA kêu gọi hành động mở link ngoài.
class AdPlayerWidget extends ConsumerStatefulWidget {
  final AdModel ad;

  const AdPlayerWidget({
    super.key,
    required this.ad,
  });

  @override
  ConsumerState<AdPlayerWidget> createState() => _AdPlayerWidgetState();
}

class _AdPlayerWidgetState extends ConsumerState<AdPlayerWidget> {
  VideoPlayerController? _controller;
  bool _isVisible = false;

  Timer? _viewTimer;
  bool _hasLoggedImpression = false;
  bool _hasLoggedView = false;

  @override
  void initState() {
    super.initState();
  }

  void _initController() {
    if (_controller != null) return;

    _controller = VideoPlayerController.networkUrl(
      Uri.parse(widget.ad.videoUri),
    );
    _controller!.initialize().then((_) {
      if (mounted) {
        _controller!.setLooping(true);
        final isMuted = ref.read(videoMuteProvider);
        _controller!.setVolume(isMuted ? 0 : 1);
        
        if (_isVisible) {
          _controller!.play();
        }
        setState(() {});
      }
    });
  }

  void _disposeController() {
    if (_controller != null) {
      _controller!.dispose();
      _controller = null;
      setState(() {});
    }
  }

  @override
  void dispose() {
    _viewTimer?.cancel();
    _controller?.dispose();
    super.dispose();
  }

  void _handleTap() {
    if (_controller == null || !_controller!.value.isInitialized) return;
    setState(() {
      if (_controller!.value.isPlaying) {
        _controller!.pause();
      } else {
        _controller!.play();
      }
    });
  }

  Future<void> _launchCtaUrl() async {
    final currentUserId = ref.read(currentUserProvider)?.uid ?? 'guest';
    ref.read(videoRepositoryProvider).logAdClick(
          adId: widget.ad.adId,
          userId: currentUserId,
        );

    String targetUrl = widget.ad.targetUrl.trim();
    if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://')) {
      targetUrl = 'https://$targetUrl';
    }

    final url = Uri.parse(targetUrl);
    try {
      final success = await launchUrl(url, mode: LaunchMode.externalApplication);
      if (!success && mounted) {
        throw Exception('Không thể mở URL');
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể mở liên kết nhà tài trợ.')),
        );
      }
    }
  }

  void _showReportDialog(BuildContext context) {
    final reasons = [
      'Nội dung phản cảm, bạo lực',
      'Thông tin lừa đảo, giả mạo',
      'Vi phạm bản quyền',
      'Spam hoặc quảng cáo quá nhiều',
      'Lý do khác',
    ];
    String selectedReason = reasons[0];
    final detailsController = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setStateDialog) {
            return AlertDialog(
              backgroundColor: const Color(0xFF1E1E1E),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              title: const Text(
                'Báo cáo quảng cáo',
                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Chọn lý do báo cáo:',
                      style: TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                    const SizedBox(height: 8),
                    ...reasons.map((reason) {
                      final isSelected = selectedReason == reason;
                      return InkWell(
                        onTap: () {
                          setStateDialog(() {
                            selectedReason = reason;
                          });
                        },
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 8.0),
                          child: Row(
                            children: [
                              Container(
                                width: 20,
                                height: 20,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: isSelected ? AppTheme.primaryColor : Colors.grey,
                                    width: 2,
                                  ),
                                ),
                                child: isSelected
                                    ? const Center(
                                        child: Icon(
                                          Icons.circle,
                                          size: 10,
                                          color: AppTheme.primaryColor,
                                        ),
                                      )
                                    : null,
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(
                                  reason,
                                  style: const TextStyle(color: Colors.white, fontSize: 14),
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                    const SizedBox(height: 12),
                    const Text(
                      'Chi tiết bổ sung (tùy chọn):',
                      style: TextStyle(color: Colors.grey, fontSize: 13),
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: detailsController,
                      maxLines: 3,
                      style: const TextStyle(color: Colors.white, fontSize: 14),
                      decoration: InputDecoration(
                        hintText: 'Nhập thông tin chi tiết...',
                        hintStyle: const TextStyle(color: Colors.grey, fontSize: 13),
                        enabledBorder: OutlineInputBorder(
                          borderSide: const BorderSide(color: Colors.white24),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderSide: const BorderSide(color: AppTheme.primaryColor),
                          borderRadius: BorderRadius.circular(8),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(ctx).pop(),
                  child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
                ),
                TextButton(
                  onPressed: () async {
                    final details = detailsController.text.trim();
                    final currentUserId = ref.read(currentUserProvider)?.uid ?? 'guest';
                    
                    try {
                      await FirebaseFirestore.instance
                          .collection(AppConstants.reportsCollection)
                          .add({
                        'reporterId': currentUserId,
                        'targetType': 'video_ad',
                        'targetId': widget.ad.adId,
                        'reason': selectedReason,
                        'details': details,
                        'status': 'pending',
                        'createdAt': DateTime.now().millisecondsSinceEpoch,
                        'timestamp': FieldValue.serverTimestamp(),
                      });
                      
                      if (context.mounted) {
                        Navigator.of(ctx).pop();
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Báo cáo của bạn đã được gửi. Cảm ơn phản hồi từ bạn!')),
                        );
                      }
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('Lỗi khi gửi báo cáo: $e')),
                        );
                      }
                    }
                  },
                  child: const Text(
                    'Gửi',
                    style: TextStyle(color: AppTheme.primaryColor, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isMuted = ref.watch(videoMuteProvider);
    if (_controller != null && _controller!.value.isInitialized) {
      _controller!.setVolume(isMuted ? 0 : 1);
    }

    final hasController = _controller != null && _controller!.value.isInitialized;

    return VisibilityDetector(
      key: Key('ad_vis_${widget.ad.adId}'),
      onVisibilityChanged: (info) {
        if (!mounted) return;
        
        final visible = info.visibleFraction > 0.5;
        if (visible != _isVisible) {
          _isVisible = visible;
          if (_isVisible) {
            if (_controller == null) {
              _initController();
            } else if (_controller!.value.isInitialized) {
              _controller!.play();
            }

            if (!_hasLoggedImpression) {
              final currentUserId = ref.read(currentUserProvider)?.uid ?? 'guest';
              ref.read(videoRepositoryProvider).logAdImpression(
                    adId: widget.ad.adId,
                    userId: currentUserId,
                  );
              _hasLoggedImpression = true;
            }

            _viewTimer?.cancel();
            _viewTimer = Timer(const Duration(seconds: 2), () {
              if (mounted && !_hasLoggedView) {
                final currentUserId = ref.read(currentUserProvider)?.uid ?? 'guest';
                ref.read(videoRepositoryProvider).logAdView(
                      adId: widget.ad.adId,
                      userId: currentUserId,
                      durationMs: 2000,
                    );
                _hasLoggedView = true;
              }
            });
          } else {
            if (_controller != null && _controller!.value.isInitialized) {
              _controller!.pause();
            }
            _viewTimer?.cancel();
          }
        }

        if (info.visibleFraction == 0.0) {
          _disposeController();
          _viewTimer?.cancel();
          _hasLoggedImpression = false;
          _hasLoggedView = false;
        }
      },
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Trình phát Video
          GestureDetector(
            onTap: _handleTap,
            child: hasController
                ? Center(
                    child: AspectRatio(
                      aspectRatio: _controller!.value.aspectRatio,
                      child: VideoPlayer(_controller!),
                    ),
                  )
                : Container(
                    color: Colors.black,
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        if (widget.ad.thumbnail.isNotEmpty)
                          Image.network(
                            widget.ad.thumbnail,
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) =>
                                const SizedBox.shrink(),
                          ),
                        const Center(
                          child: CircularProgressIndicator(
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white70),
                          ),
                        ),
                      ],
                    ),
                  ),
          ),

          // Hiển thị icon Pause/Play ở giữa màn hình nếu đang tạm dừng thủ công
          if (hasController && !_controller!.value.isPlaying)
            Align(
              alignment: Alignment.center,
              child: IgnorePointer(
                child: Semantics(
                  label: 'Phát quảng cáo',
                  button: true,
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: const BoxDecoration(
                      color: Colors.black45,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.play_arrow,
                      size: 50,
                      color: Colors.white,
                    ),
                  ),
                ),
              ),
            ),

          // Vòng xoay tải nếu video đang buffer
          if (hasController && _controller!.value.isBuffering)
            const Align(
              alignment: Alignment.center,
              child: CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Colors.white70),
              ),
            ),

          // Thanh tiến trình phát ở cuối video
          if (hasController)
            Positioned(
              left: 0,
              right: 0,
              bottom: 52,
              child: VideoProgressIndicator(
                _controller!,
                allowScrubbing: true,
                colors: const VideoProgressColors(
                  playedColor: AppTheme.primaryColor,
                  bufferedColor: Colors.white10,
                  backgroundColor: Colors.transparent,
                ),
              ),
            ),

          Positioned(
            left: 16,
            bottom: 62,
            right: 90,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.blueAccent.withValues(alpha: 0.9),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'Được tài trợ',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '@${widget.ad.sponsorName}',
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                        shadows: [Shadow(blurRadius: 4, color: Colors.black54)],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),

                Text(
                  widget.ad.description,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                    shadows: [Shadow(blurRadius: 4, color: Colors.black54)],
                  ),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),

          Positioned(
            right: 12,
            bottom: 80,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 1.5),
                    color: Colors.white.withValues(alpha: 0.1),
                    image: widget.ad.thumbnail.isNotEmpty
                        ? DecorationImage(
                            image: NetworkImage(widget.ad.thumbnail),
                            fit: BoxFit.cover,
                          )
                        : null,
                  ),
                  child: widget.ad.thumbnail.isEmpty
                      ? const Center(
                          child: Icon(Icons.business_rounded, color: Colors.white, size: 22),
                        )
                      : null,
                ),
                const SizedBox(height: 24),

                IconButton(
                  icon: const Icon(Icons.open_in_new_rounded, color: Colors.white, size: 30),
                  onPressed: _launchCtaUrl,
                ),
                const Text(
                  'Mở',
                  style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 20),

                IconButton(
                  icon: const Icon(Icons.report_gmailerrorred_rounded, color: Colors.white, size: 30),
                  onPressed: () => _showReportDialog(context),
                ),
                const Text(
                  'Báo cáo',
                  style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),

          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: GestureDetector(
              onTap: _launchCtaUrl,
              child: Container(
                height: 52,
                color: Colors.blueAccent.withValues(alpha: 0.95),
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Icon(Icons.shopping_bag_outlined, color: Colors.white, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          widget.ad.ctaText,
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                    const Icon(Icons.arrow_forward_ios_rounded, color: Colors.white, size: 16),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
