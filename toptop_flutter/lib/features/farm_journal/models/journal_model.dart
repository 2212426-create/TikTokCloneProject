/// Model Nhật ký mùa vụ / vườn trồng
class JournalModel {
  final String id;
  final String ownerId;
  final String ownerName;
  final String name;
  final String farmLocation;
  final String crop;
  final String? variety;
  final int startDate;
  final String region;
  final String description;
  final String status;  // 'in_progress' | 'finished'
  final String privacy; // 'public' | 'private'
  final int createdAt;

  JournalModel({
    required this.id,
    required this.ownerId,
    this.ownerName = '',
    required this.name,
    required this.farmLocation,
    required this.crop,
    this.variety,
    required this.startDate,
    required this.region,
    this.description = '',
    this.status = 'in_progress',
    this.privacy = 'public',
    required this.createdAt,
  });

  factory JournalModel.fromMap(Map<String, dynamic> map, [String? docId]) {
    return JournalModel(
      id: docId ?? map['id'] as String? ?? '',
      ownerId: map['ownerId'] as String? ?? '',
      ownerName: map['ownerName'] as String? ?? '',
      name: map['name'] as String? ?? '',
      farmLocation: map['farmLocation'] as String? ?? '',
      crop: map['crop'] as String? ?? '',
      variety: map['variety'] as String?,
      startDate: (map['startDate'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      region: map['region'] as String? ?? '',
      description: map['description'] as String? ?? '',
      status: map['status'] as String? ?? 'in_progress',
      privacy: map['privacy'] as String? ?? 'public',
      createdAt: (map['createdAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'ownerId': ownerId,
        'ownerName': ownerName,
        'name': name,
        'farmLocation': farmLocation,
        'crop': crop,
        'variety': variety,
        'startDate': startDate,
        'region': region,
        'description': description,
        'status': status,
        'privacy': privacy,
        'createdAt': createdAt,
      };
}

/// Model Mục nhật ký (Entry) trong mùa vụ
class JournalEntryModel {
  final String id;
  final String journalId;
  final int recordDate; // Ngày thực tế ghi nhận
  final int postDate;   // Ngày tạo mục trên ứng dụng
  final String growthStage;
  final String title;
  final String notes;
  final String? attachedVideoUrl;
  final List<String> attachedImageUrls;
  final String privacy; // 'public' | 'private'

  JournalEntryModel({
    required this.id,
    required this.journalId,
    required this.recordDate,
    required this.postDate,
    required this.growthStage,
    required this.title,
    required this.notes,
    this.attachedVideoUrl,
    this.attachedImageUrls = const [],
    this.privacy = 'public',
  });

  factory JournalEntryModel.fromMap(Map<String, dynamic> map, [String? docId]) {
    return JournalEntryModel(
      id: docId ?? map['id'] as String? ?? '',
      journalId: map['journalId'] as String? ?? '',
      recordDate: (map['recordDate'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      postDate: (map['postDate'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      growthStage: map['growthStage'] as String? ?? '',
      title: map['title'] as String? ?? '',
      notes: map['notes'] as String? ?? '',
      attachedVideoUrl: map['attachedVideoUrl'] as String?,
      attachedImageUrls: (map['attachedImageUrls'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      privacy: map['privacy'] as String? ?? 'public',
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'journalId': journalId,
        'recordDate': recordDate,
        'postDate': postDate,
        'growthStage': growthStage,
        'title': title,
        'notes': notes,
        'attachedVideoUrl': attachedVideoUrl,
        'attachedImageUrls': attachedImageUrls,
        'privacy': privacy,
      };
}
