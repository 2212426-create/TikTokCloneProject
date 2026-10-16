/// Model hồ sơ người dùng - ánh xạ từ Firestore collection "profiles"
/// Port từ Profile.java trong Android project
class ProfileModel {
  final String userId;
  final String username;
  final String fullname;
  final String avatarUrl;
  final String bio;
  final int followers;
  final int following;
  final int likes;
  final bool isPrivate;
  final String birthdate;

  ProfileModel({
    required this.userId,
    required this.username,
    this.fullname = '',
    this.avatarUrl = '',
    this.bio = 'Toptop user',
    this.followers = 0,
    this.following = 0,
    this.likes = 0,
    this.isPrivate = false,
    this.birthdate = '',
  });

  factory ProfileModel.fromMap(Map<String, dynamic> map) {
    return ProfileModel(
      userId: map['userId'] as String? ?? '',
      username: map['username'] as String? ?? '',
      fullname: map['fullname'] as String? ?? '',
      avatarUrl: map['avatar'] as String? ?? map['avatarUrl'] as String? ?? '',
      bio: map['bio'] as String? ?? 'Toptop user',
      followers: (map['followers'] as num?)?.toInt() ?? 0,
      following: (map['following'] as num?)?.toInt() ?? 0,
      likes: (map['likes'] as num?)?.toInt() ?? 0,
      isPrivate: map['isPrivate'] as bool? ?? false,
      birthdate: map['birthdate'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() => {
        'userId': userId,
        'username': username,
        'fullname': fullname,
        'avatar': avatarUrl,
        'bio': bio,
        'followers': followers,
        'following': following,
        'likes': likes,
        'isPrivate': isPrivate,
        'birthdate': birthdate,
      };

  ProfileModel copyWith({
    String? userId,
    String? username,
    String? fullname,
    String? avatarUrl,
    String? bio,
    int? followers,
    int? following,
    int? likes,
    bool? isPrivate,
    String? birthdate,
  }) {
    return ProfileModel(
      userId: userId ?? this.userId,
      username: username ?? this.username,
      fullname: fullname ?? this.fullname,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      bio: bio ?? this.bio,
      followers: followers ?? this.followers,
      following: following ?? this.following,
      likes: likes ?? this.likes,
      isPrivate: isPrivate ?? this.isPrivate,
      birthdate: birthdate ?? this.birthdate,
    );
  }
}
