import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../auth/models/profile_model.dart';
import '../providers/profile_provider.dart';

/// Màn hình hiển thị danh sách Người theo dõi / Đang theo dõi
/// Port từ FollowListActivity.java, FollowersListFragment.java, FollowingListFragment.java
class FollowListScreen extends ConsumerWidget {
  final String userId;
  final int initialTabIndex;

  const FollowListScreen({
    super.key,
    required this.userId,
    required this.initialTabIndex,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileState = ref.watch(userProfileStreamProvider(userId));

    return DefaultTabController(
      length: 2,
      initialIndex: initialTabIndex,
      child: Scaffold(
        backgroundColor: Colors.black,
        appBar: AppBar(
          backgroundColor: Colors.black,
          elevation: 0,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            onPressed: () => context.pop(),
          ),
          title: profileState.when(
            data: (profile) => Text(
              profile?.username ?? 'Hồ sơ',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
            ),
            loading: () => const SizedBox(),
            error: (error, _) => const SizedBox(),
          ),
          bottom: const TabBar(
            indicatorColor: AppTheme.primaryColor,
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white54,
            labelStyle: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            tabs: [
              Tab(text: 'Đang theo dõi'),
              Tab(text: 'Người theo dõi'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            // Tab 0: Đang theo dõi (Following)
            _buildFollowingTab(ref),

            // Tab 1: Người theo dõi (Followers)
            _buildFollowersTab(ref),
          ],
        ),
      ),
    );
  }

  Widget _buildFollowingTab(WidgetRef ref) {
    final followingState = ref.watch(followingFutureProvider(userId));

    return followingState.when(
      data: (list) {
        if (list.isEmpty) {
          return const Center(
            child: Text(
              'Chưa theo dõi ai.',
              style: TextStyle(color: Colors.white30, fontSize: 15),
            ),
          );
        }

        return ListView.separated(
          padding: const EdgeInsets.all(8),
          itemCount: list.length,
          separatorBuilder: (context, index) => const Divider(color: Colors.white10, height: 1),
          itemBuilder: (context, index) => _buildUserRow(context, list[index]),
        );
      },
      loading: () => const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
        ),
      ),
      error: (error, _) => Center(
        child: Text('Lỗi: ${error.toString()}', style: const TextStyle(color: Colors.redAccent)),
      ),
    );
  }

  Widget _buildFollowersTab(WidgetRef ref) {
    final followersState = ref.watch(followersFutureProvider(userId));

    return followersState.when(
      data: (list) {
        if (list.isEmpty) {
          return const Center(
            child: Text(
              'Chưa có người theo dõi.',
              style: TextStyle(color: Colors.white30, fontSize: 15),
            ),
          );
        }

        return ListView.separated(
          padding: const EdgeInsets.all(8),
          itemCount: list.length,
          separatorBuilder: (context, index) => const Divider(color: Colors.white10, height: 1),
          itemBuilder: (context, index) => _buildUserRow(context, list[index]),
        );
      },
      loading: () => const Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(AppTheme.primaryColor),
        ),
      ),
      error: (error, _) => Center(
        child: Text('Lỗi: ${error.toString()}', style: const TextStyle(color: Colors.redAccent)),
      ),
    );
  }

  Widget _buildUserRow(BuildContext context, ProfileModel user) {
    return ListTile(
      leading: CircleAvatar(
        radius: 22,
        backgroundColor: Colors.grey[900],
        backgroundImage: user.avatarUrl.isNotEmpty
            ? CachedNetworkImageProvider(user.avatarUrl, maxWidth: 100)
            : const AssetImage('assets/images/default_avatar.png') as ImageProvider,
        child: user.avatarUrl.isEmpty
            ? const Icon(Icons.person, size: 22, color: Colors.white54)
            : null,
      ),
      title: Text(
        user.fullname.isNotEmpty ? user.fullname : user.username,
        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15),
      ),
      subtitle: Text(
        '@${user.username}',
        style: const TextStyle(color: Colors.white54, fontSize: 13),
      ),
      trailing: const Icon(Icons.arrow_forward_ios, color: Colors.white24, size: 14),
      onTap: () {
        context.push('/user/${user.userId}');
      },
    );
  }
}
