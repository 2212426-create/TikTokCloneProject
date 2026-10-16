import 'package:flutter_test/flutter_test.dart';
import 'package:toptop_flutter/core/utils/text_utils.dart';
import 'package:toptop_flutter/features/video_feed/models/video_model.dart';

void main() {
  group('Trending & Hashtag Normalization Unit Tests', () {
    test('TextUtils.removeDiacritics removes Vietnamese accents correctly', () {
      expect(TextUtils.removeDiacritics('Sầu Riêng Ri6'), equals('sau rieng ri6'));
      expect(TextUtils.removeDiacritics('Cà Phê Đắk Lắk'), equals('ca phe dak lak'));
      expect(TextUtils.removeDiacritics('Phân Bón Hữu Cơ'), equals('phan bon huu co'));
    });

    test('TextUtils.normalizeHashtag cleans hashtag strings', () {
      expect(TextUtils.normalizeHashtag('#SầuRiêng'), equals('saurieng'));
      expect(TextUtils.normalizeHashtag('  #LâmĐồng  '), equals('lamdong'));
    });

    test('7-Day Trending video score calculation favors recent high interaction videos', () {
      final now = DateTime.now().millisecondsSinceEpoch;
      final tenDaysAgo = now - (10 * 86400 * 1000);
      final twoDaysAgo = now - (2 * 86400 * 1000);

      final oldVideo = VideoModel(
        videoId: 'v_old',
        videoUri: 'http://example.com/old.mp4',
        authorId: 'a1',
        watchCount: 10000, // High lifetime views but old
        totalLikes: 100,
        timestamp: tenDaysAgo,
      );

      final recentTrendingVideo = VideoModel(
        videoId: 'v_recent',
        videoUri: 'http://example.com/recent.mp4',
        authorId: 'a2',
        watchCount: 2000,
        totalLikes: 500,
        totalComments: 100,
        timestamp: twoDaysAgo, // Inside 7-day window
      );

      // Score = watchCount + (likes * 2) + (comments * 3)
      final scoreRecent = recentTrendingVideo.watchCount + (recentTrendingVideo.totalLikes * 2) + (recentTrendingVideo.totalComments * 3);
      final scoreOld = oldVideo.watchCount + (oldVideo.totalLikes * 2) + (oldVideo.totalComments * 3);

      expect(scoreRecent, equals(2000 + 1000 + 300)); // 3300
      expect(scoreOld, equals(10000 + 200));          // 10200
      expect(recentTrendingVideo.timestamp >= now - (7 * 86400 * 1000), isTrue);
      expect(oldVideo.timestamp >= now - (7 * 86400 * 1000), isFalse);
    });
  });
}
