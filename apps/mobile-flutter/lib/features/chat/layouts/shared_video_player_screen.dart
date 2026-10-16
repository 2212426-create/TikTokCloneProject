import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../video_feed/layouts/single_video_screen.dart';
import '../providers/chat_provider.dart';

/// Màn hình xem video fullscreen từ danh sách video được chia sẻ trong chat.
/// Hỗ trợ vuốt dọc để chuyển sang các video khác đã được share trong cùng cuộc hội thoại.
class SharedVideoPlayerScreen extends ConsumerStatefulWidget {
  final String roomId;
  final String initialVideoId;

  const SharedVideoPlayerScreen({
    super.key,
    required this.roomId,
    required this.initialVideoId,
  });

  @override
  ConsumerState<SharedVideoPlayerScreen> createState() => _SharedVideoPlayerScreenState();
}

class _SharedVideoPlayerScreenState extends ConsumerState<SharedVideoPlayerScreen> {
  late PageController _pageController;
  List<String> _videoIds = [];

  @override
  void initState() {
    super.initState();
    _pageController = PageController();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Lắng nghe stream tin nhắn của room này
    final messagesAsync = ref.watch(chatMessagesProvider(widget.roomId));

    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          messagesAsync.when(
            data: (messages) {
              // Lọc lấy danh sách videoId từ các tin nhắn type 'video_share'
              final videoIds = messages
                  .where((m) => m.type == 'video_share')
                  .map((m) => m.metadata?['videoId'] as String? ?? '')
                  .where((id) => id.isNotEmpty)
                  .toList()
                  .reversed
                  .toList(); // Đảo ngược để video mới nhất ở trên cùng/đầu danh sách

              if (videoIds.isEmpty) {
                return const Center(
                  child: Text('Không tìm thấy video nào.', 
                    style: TextStyle(color: Colors.white)),
                );
              }

              // Cập nhật local state và jump to initial
              if (_videoIds.isEmpty) {
                _videoIds = videoIds;
                final initialIndex = _videoIds.indexOf(widget.initialVideoId);
                if (initialIndex != -1) {
                  WidgetsBinding.instance.addPostFrameCallback((_) {
                    _pageController.jumpToPage(initialIndex);
                  });
                }
              }

              return PageView.builder(
                controller: _pageController,
                scrollDirection: Axis.vertical,
                itemCount: _videoIds.length,
                itemBuilder: (context, index) {
                  return SingleVideoScreen(
                    videoId: _videoIds[index],
                    isEmbedded: true,
                  );
                },
              );
            },
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(child: Text('Lỗi tải video: $e', 
              style: const TextStyle(color: Colors.white))),
          ),

          // Nút back
          Positioned(
            top: MediaQuery.of(context).padding.top + 10,
            left: 10,
            child: IconButton(
              icon: const Icon(Icons.arrow_back, color: Colors.white, size: 30),
              onPressed: () => context.pop(),
            ),
          ),
        ],
      ),
    );
  }
}
