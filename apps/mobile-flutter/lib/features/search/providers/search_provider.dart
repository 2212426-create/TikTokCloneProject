import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../core/providers/firebase_providers.dart';
import '../../auth/providers/auth_provider.dart';
import '../../auth/models/profile_model.dart';
import '../../video_feed/models/video_model.dart';
import '../../video_feed/providers/video_provider.dart';
import '../repositories/search_repository.dart';

/// Provider cho SearchRepository
final searchRepositoryProvider = Provider<SearchRepository>((ref) {
  return SearchRepository(
    firestore: ref.watch(firestoreProvider),
  );
});

/// StateProvider lưu trữ từ khóa tìm kiếm hiện tại
final searchQueryProvider = StateProvider<String>((ref) => '');

/// StateNotifier quản lý lịch sử tìm kiếm người dùng (lưu trữ trong SharedPreferences)
class SearchHistoryNotifier extends StateNotifier<List<String>> {
  SearchHistoryNotifier() : super([]) {
    _loadHistory();
  }

  static const String _prefsKey = 'search_history';

  /// Tải lịch sử từ bộ nhớ cục bộ
  Future<void> _loadHistory() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final history = prefs.getStringList(_prefsKey) ?? [];
      state = history;
    } catch (_) {}
  }

  /// Thêm một từ khóa tìm kiếm mới vào lịch sử
  Future<void> addQuery(String query, WidgetRef? ref) async {
    final cleanQuery = query.trim();
    if (cleanQuery.isEmpty) return;

    final newList = List<String>.from(state);
    newList.remove(cleanQuery); // Xóa nếu đã tồn tại để đưa lên đầu
    newList.insert(0, cleanQuery); // Thêm vào đầu danh sách
    
    if (newList.length > 10) {
      newList.removeLast(); // Giới hạn tối đa 10 mục
    }

    state = newList;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(_prefsKey, newList);

    // Ghi nhận sở thích từ tìm kiếm (Search weight: 2)
    if (ref != null) {
      final user = ref.read(currentUserProvider);
      if (user != null) {
        ref.read(videoRepositoryProvider).recordInterest(
          tags: [cleanQuery], // Coi từ khóa là 1 tag tiềm năng
          currentUid: user.uid,
          weight: 2,
        );
      }
    }
  }

  /// Xóa toàn bộ lịch sử tìm kiếm
  Future<void> clearHistory() async {
    state = [];
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_prefsKey);
  }
}

/// Provider lịch sử tìm kiếm
final searchHistoryProvider =
    StateNotifierProvider<SearchHistoryNotifier, List<String>>((ref) {
  return SearchHistoryNotifier();
});

/// FutureProvider danh sách hashtag thịnh hành
final trendingTagsProvider = FutureProvider<List<String>>((ref) {
  return ref.watch(searchRepositoryProvider).loadTrendingTags();
});

/// FutureProvider danh sách video thịnh hành (khi ô tìm kiếm trống)
final trendingVideosProvider = FutureProvider<List<VideoModel>>((ref) {
  return ref.watch(searchRepositoryProvider).loadTrendingVideos();
});

/// FutureProvider danh sách kết quả tìm kiếm người dùng
final userSearchResultsProvider =
    FutureProvider.family<List<ProfileModel>, String>((ref, query) {
  if (query.trim().isEmpty) return const [];
  return ref.watch(searchRepositoryProvider).searchUsers(query);
});

/// FutureProvider danh sách kết quả tìm kiếm video
final videoSearchResultsProvider =
    FutureProvider.family<List<VideoModel>, String>((ref, query) {
  if (query.trim().isEmpty) return const [];
  return ref.watch(searchRepositoryProvider).searchVideos(query);
});
