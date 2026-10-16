import 'video_model.dart';
import 'ad_model.dart';

/// Lớp trừu tượng biểu diễn một phần tử xuất hiện trên feed của người dùng
abstract class FeedItem {
  final String id;
  FeedItem(this.id);
}

/// Phần tử video thường
class VideoItem extends FeedItem {
  final VideoModel video;
  VideoItem(this.video) : super(video.videoId);
}

/// Phần tử video quảng cáo
class AdItem extends FeedItem {
  final AdModel ad;
  AdItem(this.ad) : super(ad.adId);
}
