/// Model đại diện cho một video quảng cáo (Sponsored Video Ads)
class AdModel {
  final String adId;
  final String videoUri;
  final String sponsorName;
  final String description;
  final String ctaText;
  final String targetUrl;
  final String thumbnail;
  final String status;
  final int startDate;
  final int endDate;

  AdModel({
    required this.adId,
    required this.videoUri,
    required this.sponsorName,
    required this.description,
    this.ctaText = 'Tìm hiểu thêm',
    required this.targetUrl,
    required this.thumbnail,
    required this.status,
    required this.startDate,
    required this.endDate,
  });

  factory AdModel.fromMap(Map<String, dynamic> map, String id) {
    return AdModel(
      adId: id,
      videoUri: map['videoUrl'] as String? ?? map['videoUri'] as String? ?? '',
      sponsorName: map['advertiserName'] as String? ?? map['sponsorName'] as String? ?? 'Tài trợ',
      description: map['description'] as String? ?? '',
      ctaText: map['ctaText'] as String? ?? 'Tìm hiểu thêm',
      targetUrl: map['targetUrl'] as String? ?? 'https://google.com',
      thumbnail: map['thumbnail'] as String? ?? map['thumbnailUri'] as String? ?? '',
      status: map['status'] as String? ?? 'active',
      startDate: (map['startDate'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      endDate: (map['endDate'] as num?)?.toInt() ?? (DateTime.now().millisecondsSinceEpoch + 30 * 24 * 60 * 60 * 1000),
    );
  }

  Map<String, dynamic> toMap() => {
        'videoUrl': videoUri,
        'videoUri': videoUri,
        'advertiserName': sponsorName,
        'sponsorName': sponsorName,
        'description': description,
        'ctaText': ctaText,
        'targetUrl': targetUrl,
        'thumbnail': thumbnail,
        'status': status,
        'startDate': startDate,
        'endDate': endDate,
      };
}

