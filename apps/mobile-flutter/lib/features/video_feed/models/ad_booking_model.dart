/// Model đại diện cho một lượt đặt quảng cáo (Ad Booking)
class AdBookingModel {
  final String bookingId;
  final String advertiserId;
  final String title;
  final String description;
  final String videoUri;
  final String targetUrl;
  final double requestedBudget;
  final int createdAt;
  final String status;
  final String rejectReason;

  AdBookingModel({
    required this.bookingId,
    required this.advertiserId,
    required this.title,
    required this.description,
    required this.videoUri,
    required this.targetUrl,
    required this.requestedBudget,
    required this.createdAt,
    required this.status,
    this.rejectReason = '',
  });

  factory AdBookingModel.fromMap(Map<String, dynamic> map, String id) {
    return AdBookingModel(
      bookingId: id,
      advertiserId: map['advertiserId'] as String? ?? '',
      title: map['title'] as String? ?? '',
      description: map['description'] as String? ?? '',
      videoUri: map['videoUri'] as String? ?? '',
      targetUrl: map['targetUrl'] as String? ?? '',
      requestedBudget: (map['requestedBudget'] as num?)?.toDouble() ?? 0.0,
      createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
      status: map['status'] as String? ?? 'pending',
      rejectReason: map['rejectReason'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() => {
        'advertiserId': advertiserId,
        'title': title,
        'description': description,
        'videoUri': videoUri,
        'targetUrl': targetUrl,
        'requestedBudget': requestedBudget,
        'createdAt': createdAt,
        'status': status,
        'rejectReason': rejectReason,
      };
}
