/// Model đại diện cho một nhà quảng cáo (Advertiser)
class AdvertiserModel {
  final String advertiserId;
  final String companyName;
  final String contactEmail;
  final double balance;
  final String status;

  AdvertiserModel({
    required this.advertiserId,
    required this.companyName,
    required this.contactEmail,
    required this.balance,
    required this.status,
  });

  factory AdvertiserModel.fromMap(Map<String, dynamic> map, String id) {
    return AdvertiserModel(
      advertiserId: id,
      companyName: map['companyName'] as String? ?? '',
      contactEmail: map['contactEmail'] as String? ?? '',
      balance: (map['balance'] as num?)?.toDouble() ?? 0.0,
      status: map['status'] as String? ?? 'active',
    );
  }

  Map<String, dynamic> toMap() => {
        'companyName': companyName,
        'contactEmail': contactEmail,
        'balance': balance,
        'status': status,
      };
}
