// ignore_for_file: prefer_initializing_formals
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:http/http.dart' as http;
import '../../../core/constants/app_constants.dart';
import '../../video_feed/models/video_model.dart';
import '../services/gemini_service.dart';

/// Hỗ trợ quét và tự động sửa các hashtag bị thiếu bằng Gemini AI
class LegacyHashtagFixer {
  final FirebaseFirestore _firestore;
  final GeminiService _geminiService;

  LegacyHashtagFixer({
    required FirebaseFirestore firestore,
    required GeminiService geminiService,
  })  : _firestore = firestore,
        _geminiService = geminiService;

  /// Quét tất cả video, tìm video không có hashtag/dấu # và tự động bổ sung bằng AI
  Future<void> fixMissingHashtags() async {
    try {
      final snapshot = await _firestore.collection(AppConstants.videosCollection).get();
      for (final doc in snapshot.docs) {
        final video = VideoModel.fromMap(doc.data());
        // Kiểm tra xem video có bị thiếu hashtag không
        if (video.hashtags.isEmpty && !video.description.contains('#')) {
          await _fixSingleVideo(video);
        }
      }
    } catch (_) {}
  }

  /// Phân tích ảnh bìa của một video và cập nhật hashtag lên Firestore
  Future<void> _fixSingleVideo(VideoModel video) async {
    try {
      // Lấy URL ảnh bìa từ videoUri của Cloudinary làm ảnh phân tích
      String thumbUrl = 'https://picsum.photos/200/300';
      if (video.videoUri.contains('cloudinary.com')) {
        thumbUrl = video.videoUri.replaceAll('.mp4', '.jpg');
        if (thumbUrl.contains('/upload/')) {
          thumbUrl = thumbUrl.replaceAll('/upload/', '/upload/so_0/');
        }
      }

      // Tải bytes ảnh từ Cloudinary
      final response = await http.get(Uri.parse(thumbUrl));
      if (response.statusCode == 200) {
        final imageBytes = response.bodyBytes;
        
        // Gọi Gemini gợi ý hashtag
        final hashtagString = await _geminiService.suggestHashtags(imageBytes);
        
        if (hashtagString.isNotEmpty) {
          final List<String> tags = [];
          final matches = RegExp(r'#([A-Za-z0-9_\u00C0-\u1EF9-]+)').allMatches(hashtagString);
          for (final m in matches) {
            final tagText = m.group(1);
            if (tagText != null) {
              tags.add(tagText.toLowerCase());
            }
          }

          final updatedDesc = '${video.description} $hashtagString'.trim();
          
          // Cập nhật lên Firestore
          await _firestore.collection(AppConstants.videosCollection).doc(video.videoId).update({
            'description': updatedDesc,
            'hashtags': tags,
          });
        }
      }
    } catch (_) {}
  }
}
