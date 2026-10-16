// ignore_for_file: deprecated_member_use
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/shimmer_loading.dart';
import '../../auth/models/profile_model.dart';
import '../../auth/providers/auth_provider.dart';
import '../../video_feed/models/video_model.dart';
import '../providers/search_provider.dart';
import '../widgets/trending_tags.dart';

/// Màn hình tìm kiếm nâng cao với tab phân loại kết quả (Người dùng / Video)
/// Hỗ trợ hiển thị thẻ từ khóa thịnh hành, lịch sử tìm kiếm cục bộ và danh sách video đề cử.
class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}


class _SearchScreenState extends ConsumerState<SearchScreen>
    with SingleTickerProviderStateMixin {
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  TabController? _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    
    // Đồng bộ controller với state hiện tại (nếu có)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final currentQuery = ref.read(searchQueryProvider);
      if (currentQuery.isNotEmpty) {
        _searchController.text = currentQuery;
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _focusNode.dispose();
    _tabController?.dispose();
    super.dispose();
  }

  void _onSearchSubmit(String text) {
    final cleanText = text.trim();
    if (cleanText.isEmpty) return;

    ref.read(searchQueryProvider.notifier).state = cleanText;
    ref.read(searchHistoryProvider.notifier).addQuery(cleanText, ref);
    _focusNode.unfocus();
  }

  void _onTagSelect(String tag) {
    _searchController.text = tag;
    ref.read(searchQueryProvider.notifier).state = tag;
    ref.read(searchHistoryProvider.notifier).addQuery(tag, ref);
    _focusNode.unfocus();
  }

  void _clearSearch() {
    _searchController.clear();
    ref.read(searchQueryProvider.notifier).state = '';
    _focusNode.requestFocus();
  }

  @override
  Widget build(BuildContext context) {
    final query = ref.watch(searchQueryProvider);
    final searchHistory = ref.watch(searchHistoryProvider);
    final trendingTagsAsync = ref.watch(trendingTagsProvider);
    final trendingVideosAsync = ref.watch(trendingVideosProvider);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      body: SafeArea(
        child: Column(
          children: [
            // 1. Ô tìm kiếm trên cùng
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 8.0),
              child: Row(
                children: [
                  // Nút back arrow màu TikTok Red
                  IconButton(
                    icon: const Icon(
                      Icons.arrow_back,
                      color: AppTheme.primaryColor,
                    ),
                    onPressed: () {
                      if (query.isNotEmpty) {
                        _clearSearch();
                      } else {
                        // Về Tab 0 (Home Feed)
                        context.go('/home');
                      }
                    },
                  ),

                  // Input box
                  Expanded(
                    child: Container(
                      height: 40,
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.12),
                          width: 1,
                        ),
                      ),
                      child: TextField(
                        controller: _searchController,
                        focusNode: _focusNode,
                        style: const TextStyle(color: Colors.white, fontSize: 14),
                        textInputAction: TextInputAction.search,
                        onSubmitted: _onSearchSubmit,
                        decoration: InputDecoration(
                          hintText: 'Tìm người dùng hoặc #tag...',
                          hintStyle: const TextStyle(
                            color: AppTheme.textHint,
                            fontSize: 14,
                          ),
                          prefixIcon: const Icon(
                            Icons.search,
                            color: AppTheme.primaryColor,
                            size: 18,
                          ),
                          suffixIcon: _searchController.text.isNotEmpty
                              ? GestureDetector(
                                  onTap: _clearSearch,
                                  child: const Icon(
                                    Icons.cancel,
                                    color: Colors.white54,
                                    size: 16,
                                  ),
                                )
                              : null,
                          filled: false,
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 10,
                          ),
                        ),
                        onChanged: (val) {
                          setState(() {});
                          if (val.trim().isEmpty) {
                            ref.read(searchQueryProvider.notifier).state = '';
                          }
                        },
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),

                  // Nút Tìm kiếm
                  GestureDetector(
                    onTap: () => _onSearchSubmit(_searchController.text),
                    child: const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8.0),
                      child: Text(
                        'Tìm kiếm',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),

            // 2. Nội dung hiển thị tùy thuộc vào từ khóa rỗng hay không
            Expanded(
              child: query.isEmpty
                  ? _buildDiscoveryUI(
                      searchHistory: searchHistory,
                      trendingTags: trendingTagsAsync,
                      trendingVideos: trendingVideosAsync,
                    )
                  : _buildSearchResultsUI(query),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCampaignBanner(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      height: 120,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: const LinearGradient(
          colors: [Color(0xFFFE2C55), Color(0xFF8B2C4E), Color(0xFF1D1D2F)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: const [
          BoxShadow(
            color: Colors.black26,
            blurRadius: 6,
            offset: Offset(0, 3),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () {
            _onTagSelect('dalat');
          },
          child: Stack(
            children: [
              Positioned(
                right: -10,
                bottom: -10,
                child: Opacity(
                  opacity: 0.25,
                  child: const Text('🌲', style: TextStyle(fontSize: 100)),
                ),
              ),
              Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'CHIẾN DỊCH QUẢNG BÁ',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1.0,
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Đà Lạt Trong Tôi 🌲',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Khám phá xứ sở sương mù & lưu lại khoảnh khắc',
                      style: TextStyle(
                        color: Colors.white70,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // Giao diện Khám phá (khi chưa nhập gì)
  Widget _buildDiscoveryUI({
    required List<String> searchHistory,
    required AsyncValue<List<String>> trendingTags,
    required AsyncValue<List<VideoModel>> trendingVideos,
  }) {
    return ListView(
      children: [
        _buildCampaignBanner(context),
        
        // 2.1 Lịch sử tìm kiếm
        if (searchHistory.isNotEmpty) ...[
          Padding(
            padding: const EdgeInsets.only(left: 16, right: 16, top: 12, bottom: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Lịch sử tìm kiếm',
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                GestureDetector(
                  onTap: () =>
                      ref.read(searchHistoryProvider.notifier).clearHistory(),
                  child: const Icon(
                    Icons.delete_outline,
                    color: Colors.grey,
                    size: 18,
                  ),
                ),
              ],
            ),
          ),
          SizedBox(
            height: 36,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: searchHistory.length,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemBuilder: (context, index) {
                final hist = searchHistory[index];
                return Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: GestureDetector(
                    onTap: () => _onTagSelect(hist),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white.withValues(alpha: 0.10),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.15),
                          width: 1,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.history,
                            color: Colors.white54,
                            size: 13,
                          ),
                          const SizedBox(width: 5),
                          Text(
                            hist,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const SizedBox(height: 12),
        ],

        // 2.2 Hashtags thịnh hành
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: Row(
            children: [
              Icon(Icons.trending_up, color: AppTheme.primaryColor, size: 18),
              const SizedBox(width: 6),
              const Text(
                'Hashtag thịnh hành',
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
        trendingTags.when(
          data: (tags) => TrendingTags(
            tags: tags,
            onTagTap: _onTagSelect,
          ),
          loading: () => const SizedBox(
            height: 38,
            child: Center(
              child: SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.primaryColor),
              ),
            ),
          ),
          error: (err, stack) => const SizedBox.shrink(),
        ),
        const SizedBox(height: 16),

        // 2.3 Video thịnh hành đề xuất
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: Row(
            children: [
              Icon(Icons.explore_outlined, color: AppTheme.secondaryColor, size: 18),
              const SizedBox(width: 6),
              const Text(
                'Khám phá video',
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
        trendingVideos.when(
          data: (videos) {
            if (videos.isEmpty) {
              return const Center(
                child: Padding(
                  padding: EdgeInsets.symmetric(vertical: 40.0),
                  child: Text('Chưa có video nào', style: TextStyle(color: Colors.grey)),
                ),
              );
            }
            return GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 2,
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 0.65,
              ),
              itemCount: videos.length,
              itemBuilder: (context, index) {
                final video = videos[index];
                return _VideoGridItem(video: video);
              },
            );
          },
          loading: () => const ShimmerVideoGrid(),
          error: (err, _) => Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 40.0),
              child: Text('Lỗi tải dữ liệu: $err', style: const TextStyle(color: Colors.red)),
            ),
          ),
        ),
        const SizedBox(height: 80), // Chừa khoảng trống tránh bị đè bởi BottomNavigationBar
      ],
    );
  }

  // Giao diện kết quả tìm kiếm (khi đã nhập từ khóa)
  Widget _buildSearchResultsUI(String queryKey) {
    return Column(
      children: [
        // TabBar chọn phân loại
        TabBar(
          controller: _tabController,
          indicatorColor: AppTheme.primaryColor,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.grey,
          tabs: const [
            Tab(text: 'Người dùng'),
            Tab(text: 'Videos'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              // Tab người dùng
              _buildUsersSearchTab(queryKey),

              // Tab videos
              _buildVideosSearchTab(queryKey),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildUsersSearchTab(String queryKey) {
    final usersAsync = ref.watch(userSearchResultsProvider(queryKey));

    return usersAsync.when(
      data: (profiles) {
        if (profiles.isEmpty) {
          return const Center(
            child: Text(
              'Không tìm thấy người dùng nào',
              style: TextStyle(color: Colors.grey),
            ),
          );
        }
        return ListView.builder(
          itemCount: profiles.length,
          padding: const EdgeInsets.only(top: 8, bottom: 80),
          itemBuilder: (context, index) {
            final profile = profiles[index];
            return _UserSearchTile(profile: profile);
          },
        );
      },
      loading: () => const ShimmerInboxList(),
      error: (err, _) => Center(
        child: Text('Lỗi: $err', style: const TextStyle(color: Colors.red)),
      ),
    );
  }

  Widget _buildVideosSearchTab(String queryKey) {
    final videosAsync = ref.watch(videoSearchResultsProvider(queryKey));

    return videosAsync.when(
      data: (videos) {
        if (videos.isEmpty) {
          return const Center(
            child: Text(
              'Không tìm thấy video nào',
              style: TextStyle(color: Colors.grey),
            ),
          );
        }
        return GridView.builder(
          padding: const EdgeInsets.only(left: 12, right: 12, top: 12, bottom: 80),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 2,
            crossAxisSpacing: 10,
            mainAxisSpacing: 10,
            childAspectRatio: 0.65,
          ),
          itemCount: videos.length,
          itemBuilder: (context, index) {
            final video = videos[index];
            return _VideoGridItem(video: video);
          },
        );
      },
      loading: () => const ShimmerVideoGrid(),
      error: (err, _) => Center(
        child: Text('Lỗi: $err', style: const TextStyle(color: Colors.red)),
      ),
    );
  }
}

/// Dòng người dùng trong danh sách tìm kiếm
class _UserSearchTile extends ConsumerWidget {
  final ProfileModel profile;

  const _UserSearchTile({required this.profile});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUserId = ref.watch(currentUserProvider)?.uid;

    return ListTile(
      leading: CircleAvatar(
        radius: 24,
        backgroundColor: Colors.grey[850],
        backgroundImage: profile.avatarUrl.isNotEmpty
            ? CachedNetworkImageProvider(profile.avatarUrl, maxWidth: 100)
            : null,
        child: profile.avatarUrl.isEmpty
            ? const Icon(Icons.person, color: Colors.white, size: 24)
            : null,
      ),
      title: Text(
        profile.username,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 14,
          fontWeight: FontWeight.bold,
        ),
      ),
      subtitle: Text(
        profile.fullname.isNotEmpty ? profile.fullname : 'TopTop User',
        style: TextStyle(
          color: Colors.grey[600],
          fontSize: 12,
        ),
      ),
      trailing: const Icon(
        Icons.chevron_right,
        color: Colors.white30,
      ),
      onTap: () {
        if (currentUserId != null && currentUserId == profile.userId) {
          context.go('/profile');
        } else {
          context.push('/user/${profile.userId}');
        }
      },
    );
  }
}

/// Thẻ Grid hiển thị một video
class _VideoGridItem extends StatelessWidget {
  final VideoModel video;

  const _VideoGridItem({required this.video});

  @override
  Widget build(BuildContext context) {
    // Sử dụng videoUri hoặc cover nếu không có thumbnail
    String thumbUrl = 'https://picsum.photos/200/300';
    if (video.videoUri.contains('cloudinary.com')) {
      thumbUrl = video.videoUri.replaceAll('.mp4', '.jpg');
      if (thumbUrl.contains('/upload/')) {
        thumbUrl = thumbUrl.replaceAll('/upload/', '/upload/so_0/');
      }
    }

    return RepaintBoundary(
      child: GestureDetector(
        onTap: () {
          context.push('/video/${video.videoId}');
        },
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Container(
            color: Colors.grey[900],
            child: Stack(
              fit: StackFit.expand,
              children: [
                // Dynamic thumbnail from Cloudinary
                CachedNetworkImage(
                  imageUrl: thumbUrl,
                  fit: BoxFit.cover,
                  memCacheWidth: 250, // Tối ưu kích thước lưu cache bộ nhớ
                  placeholder: (context, url) => Container(
                  color: Colors.grey[900],
                  child: const Center(
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white24),
                    ),
                  ),
                ),
                errorWidget: (context, url, error) => const Center(
                  child: Icon(
                    Icons.play_circle_outline,
                    color: Colors.white30,
                    size: 40,
                  ),
                ),
              ),

              // Gradient Overlay
              Opacity(
                opacity: 0.4,
                child: Container(
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Colors.black87, Colors.transparent],
                      begin: Alignment.bottomCenter,
                      end: Alignment.topCenter,
                    ),
                  ),
                ),
              ),

              // Thống kê lượt xem ở góc dưới cùng bên trái
              Positioned(
                left: 8,
                bottom: 8,
                child: Row(
                  children: [
                    const Icon(
                      Icons.play_arrow,
                      color: Colors.white,
                      size: 14,
                    ),
                    const SizedBox(width: 2),
                    Text(
                      _formatWatchCount(video.watchCount),
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

  String _formatWatchCount(int count) {
    if (count >= 1000000) {
      return '${(count / 1000000).toStringAsFixed(1)}M';
    } else if (count >= 1000) {
      return '${(count / 1000).toStringAsFixed(1)}K';
    }
    return count.toString();
  }
}
