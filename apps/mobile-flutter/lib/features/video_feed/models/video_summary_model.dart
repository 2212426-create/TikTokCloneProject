/// Model tóm tắt video - dùng cho danh sách lưới video (ví dụ trên profile)
/// Port từ VideoSummary.java trong Android project
class VideoSummaryModel {
  final String videoId;
  final String thumbnailUri;
  final int watchCount;

  VideoSummaryModel({
    required this.videoId,
    this.thumbnailUri = '',
    this.watchCount = 0,
  });

  factory VideoSummaryModel.fromMap(Map<String, dynamic> map) {
    return VideoSummaryModel(
      videoId: map['videoId'] as String? ?? '',
      thumbnailUri: map['thumbnailUri'] as String? ?? '',
      watchCount: (map['watchCount'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toMap() => {
        'videoId': videoId,
        'thumbnailUri': thumbnailUri,
        'watchCount': watchCount,
      };
}
