import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers/firebase_providers.dart';
import '../../auth/models/profile_model.dart';
import '../../video_feed/models/video_summary_model.dart';
import '../repositories/profile_repository.dart';

/// Provider cho ProfileRepository
final profileRepositoryProvider = Provider<ProfileRepository>((ref) {
  return ProfileRepository(
    firestore: ref.watch(firestoreProvider),
    database: ref.watch(realtimeDbProvider),
    storage: ref.watch(firebaseStorageProvider),
  );
});

/// StreamProvider theo dõi chi tiết hồ sơ người dùng
final userProfileStreamProvider =
    StreamProvider.family<ProfileModel?, String>((ref, uid) {
  return ref.watch(profileRepositoryProvider).watchProfile(uid);
});

/// StreamProvider theo dõi danh sách video tóm tắt của một người dùng
final userVideosStreamProvider =
    StreamProvider.family<List<VideoSummaryModel>, String>((ref, uid) {
  return ref.watch(profileRepositoryProvider).watchUserVideos(uid);
});

/// StreamProvider theo dõi mối quan hệ follow giữa hai người dùng
/// Key truyền vào có dạng: "currentUid_targetUid"
final followStatusStreamProvider =
    StreamProvider.family<bool, String>((ref, compositeKey) {
  final parts = compositeKey.split('_');
  if (parts.length < 2) return Stream.value(false);
  final currentUid = parts[0];
  final targetUid = parts[1];
  return ref
      .watch(profileRepositoryProvider)
      .watchFollowStatus(currentUid: currentUid, targetUid: targetUid);
});

/// FutureProvider lấy danh sách người theo dõi
final followersFutureProvider =
    FutureProvider.family<List<ProfileModel>, String>((ref, uid) {
  return ref.watch(profileRepositoryProvider).getFollowers(uid);
});

/// FutureProvider lấy danh sách người đang theo dõi
final followingFutureProvider =
    FutureProvider.family<List<ProfileModel>, String>((ref, uid) {
  return ref.watch(profileRepositoryProvider).getFollowing(uid);
});
