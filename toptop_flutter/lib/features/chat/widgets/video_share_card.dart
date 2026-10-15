import 'package:cached_network_image/cached_network_image.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:video_player/video_player.dart';
import 'package:visibility_detector/visibility_detector.dart';
import '../../../core/constants/app_constants.dart';
import '../../../core/theme/app_theme.dart';

/// Widget hiển thị Video Share Card kiểu TikTok trong tin nhắn chat.
/// Hỗ trợ Auto-play muted khi xuất hiện trong viewport.
class VideoShareCard extends StatefulWidget {
  final String videoId;
  final String? roomId;

  const VideoShareCard({super.key, required this.videoId, this.roomId});

  @override
  State<VideoShareCard> createState() => _VideoShareCardState();
}

class _VideoShareCardState extends State<VideoShareCard> {
  VideoPlayerController? _controller;
  bool _isInitialized = false;
  bool _isMuted = true;
  String? _videoUrl;

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  Future<void> _initializePlayer(String url) async {
    if (_videoUrl == url) return;
    
    await _controller?.dispose();
    _videoUrl = url;
    
    _controller = VideoPlayerController.networkUrl(Uri.parse(url));
    try {
      await _controller!.initialize();
      await _controller!.setLooping(true);
      await _controller!.setVolume(0);
      if (mounted) {
        setState(() {
          _isInitialized = true;
        });
      }
    } catch (e) {
      debugPrint("Error initializing shared video player: $e");
    }
  }

  String _formatCount(int count) {
    if (count >= 1000000) return '${(count / 1000000).toStringAsFixed(1)}M';
    if (count >= 1000) return '${(count / 1000).toStringAsFixed(1)}K';
    return count.toString();
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final cardBg = isDark ? const Color(0xFF1A1A1F) : Colors.white;
    final borderColor = isDark
        ? Colors.white.withValues(alpha: 0.1)
        : Colors.black.withValues(alpha: 0.08);
    final textColor = isDark ? Colors.white : const Color(0xFF161722);
    final subColor = isDark ? Colors.white54 : Colors.black54;

    return FutureBuilder<DocumentSnapshot<Map<String, dynamic>>>(
      future: FirebaseFirestore.instance
          .collection(AppConstants.videosCollection)
          .doc(widget.videoId)
          .get(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting && !_isInitialized) {
          return _buildSkeleton(cardBg, borderColor);
        }

        final data = snapshot.data?.data();
        if (data == null && snapshot.connectionState == ConnectionState.done) {
          return _buildErrorCard(isDark, subColor);
        }

        if (data != null && !_isInitialized) {
          final url = data['videoUri'] as String? ?? '';
          if (url.isNotEmpty) {
            _initializePlayer(url);
          }
        }

        final thumbnailUrl = data?['thumbnailUri'] as String? ?? '';
        final description = data?['description'] as String? ?? 'Video TopTop';
        final authorId = data?['authorId'] as String? ?? '';
        final watchCount = (data?['watchCount'] as num?)?.toInt() ?? 0;

        return VisibilityDetector(
          key: Key('video_share_${widget.videoId}'),
          onVisibilityChanged: (info) {
            if (info.visibleFraction > 0.6) {
              _controller?.play();
            } else {
              _controller?.pause();
            }
          },
          child: GestureDetector(
            onTap: () {
              if (widget.roomId != null && widget.roomId!.isNotEmpty) {
                context.push('/shared-videos/${widget.roomId}?videoId=${widget.videoId}');
              } else {
                context.push('/video/${widget.videoId}');
              }
            },
            child: Container(
              width: 220,
              decoration: BoxDecoration(
                color: cardBg,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: borderColor),
                boxShadow: isDark
                    ? []
                    : [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.08),
                          blurRadius: 8,
                          offset: const Offset(0, 2),
                        ),
                      ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  ClipRRect(
                    borderRadius:
                        const BorderRadius.vertical(top: Radius.circular(14)),
                    child: AspectRatio(
                      aspectRatio: 9 / 14,
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          _isInitialized
                              ? VideoPlayer(_controller!)
                              : (thumbnailUrl.isNotEmpty
                                  ? CachedNetworkImage(
                                      imageUrl: thumbnailUrl,
                                      fit: BoxFit.cover,
                                    )
                                  : Container(color: Colors.black12)),

                          if (_isInitialized)
                            Positioned(
                              top: 8,
                              left: 8,
                              child: GestureDetector(
                                onTap: () {
                                  setState(() {
                                    _isMuted = !_isMuted;
                                    _controller?.setVolume(_isMuted ? 0 : 1);
                                  });
                                },
                                child: Container(
                                  padding: const EdgeInsets.all(4),
                                  decoration: BoxDecoration(
                                    color: Colors.black.withValues(alpha: 0.4),
                                    shape: BoxShape.circle,
                                  ),
                                  child: Icon(
                                    _isMuted ? Icons.volume_off : Icons.volume_up,
                                    color: Colors.white,
                                    size: 16,
                                  ),
                                ),
                              ),
                            ),

                          // Play button overlay (wrapped in IgnorePointer so tap passes to GestureDetector)
                          if (_isInitialized && !_controller!.value.isPlaying)
                            Center(
                              child: IgnorePointer(
                                child: Container(
                                  width: 48,
                                  height: 48,
                                  decoration: BoxDecoration(
                                    color: Colors.black.withValues(alpha: 0.45),
                                    shape: BoxShape.circle,
                                    border: Border.all(
                                        color: Colors.white.withValues(alpha: 0.8),
                                        width: 1.5),
                                  ),
                                  child: const Icon(Icons.play_arrow_rounded,
                                      color: Colors.white, size: 30),
                                ),
                              ),
                            ),

                          Positioned(
                            left: 0,
                            right: 0,
                            bottom: 0,
                            height: 60,
                            child: DecoratedBox(
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topCenter,
                                  end: Alignment.bottomCenter,
                                  colors: [
                                    Colors.transparent,
                                    Colors.black.withValues(alpha: 0.55),
                                  ],
                                ),
                              ),
                            ),
                          ),

                          Positioned(
                            top: 8,
                            right: 8,
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 6, vertical: 3),
                              decoration: BoxDecoration(
                                color: Colors.black.withValues(alpha: 0.55),
                                borderRadius: BorderRadius.circular(4),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Container(
                                    width: 10,
                                    height: 10,
                                    decoration: BoxDecoration(
                                      gradient: AppTheme.logoGradient,
                                      shape: BoxShape.circle,
                                    ),
                                  ),
                                  const SizedBox(width: 4),
                                  const Text(
                                    'TopTop',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 10,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),

                          Positioned(
                            left: 8,
                            bottom: 6,
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(Icons.play_arrow,
                                    color: Colors.white70, size: 14),
                                const SizedBox(width: 2),
                                Text(
                                  _formatCount(watchCount),
                                  style: const TextStyle(
                                      color: Colors.white70, fontSize: 11),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  Padding(
                    padding: const EdgeInsets.fromLTRB(10, 8, 10, 10),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: textColor,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                            height: 1.4,
                          ),
                        ),
                        const SizedBox(height: 6),

                        _AuthorRow(
                            authorId: authorId, isDark: isDark, subColor: subColor),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildSkeleton(Color bg, Color border) {
    return Container(
      width: 220,
      height: 280,
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: border),
      ),
      child: const Center(
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            valueColor:
                AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorCard(bool isDark, Color subColor) {
    return Container(
      width: 220,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF1A1A1F) : Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: isDark ? Colors.white12 : Colors.black12,
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.broken_image_outlined,
              color: Colors.grey, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Video không còn tồn tại',
              style: TextStyle(color: subColor, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

class _AuthorRow extends StatelessWidget {
  final String authorId;
  final bool isDark;
  final Color subColor;

  const _AuthorRow({
    required this.authorId,
    required this.isDark,
    required this.subColor,
  });

  @override
  Widget build(BuildContext context) {
    if (authorId.isEmpty) return const SizedBox.shrink();

    return FutureBuilder<DocumentSnapshot<Map<String, dynamic>>>(
      future: FirebaseFirestore.instance
          .collection(AppConstants.profilesCollection)
          .doc(authorId)
          .get(),
      builder: (context, snap) {
        final data = snap.data?.data();
        final username = data?['username'] as String? ?? '';
        final avatarUrl = data?['avatarUrl'] as String? ?? '';

        return Row(
          children: [
            SizedBox(
              width: 18,
              height: 18,
              child: CircleAvatar(
                radius: 9,
                backgroundColor: isDark
                    ? Colors.white.withValues(alpha: 0.1)
                    : const Color(0xFFE8E8E8),
                backgroundImage: avatarUrl.isNotEmpty
                    ? CachedNetworkImageProvider(avatarUrl, maxWidth: 40)
                    : null,
                child: avatarUrl.isEmpty
                    ? Icon(Icons.person,
                        size: 12, color: subColor)
                    : null,
              ),
            ),
            const SizedBox(width: 5),

            Expanded(
              child: Text(
                username.isNotEmpty ? '@$username' : 'TopTop User',
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: subColor,
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
