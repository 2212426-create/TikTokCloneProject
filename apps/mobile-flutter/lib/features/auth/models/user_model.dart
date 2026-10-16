/// Model người dùng - ánh xạ từ Firestore collection "users"
/// Port từ User.java trong Android project
class UserModel {
  final String userId;
  final String username;
  final String avatarUrl;
  final String email;
  final String phone;
  final String birthdate;
  final bool isPrivate;
  final int followers;
  final int following;
  final int likes;
  final String status;
  final String role;

  UserModel({
    required this.userId,
    required this.username,
    this.avatarUrl = '',
    this.email = '',
    this.phone = '',
    this.birthdate = '',
    this.isPrivate = false,
    this.followers = 0,
    this.following = 0,
    this.likes = 0,
    this.status = 'active',
    this.role = 'user',
  });

  factory UserModel.fromMap(Map<String, dynamic> map) {
    return UserModel(
      userId: map['userId'] as String? ?? '',
      username: map['username'] as String? ?? '',
      avatarUrl: map['avatarUrl'] as String? ?? '',
      email: map['email'] as String? ?? '',
      phone: map['phone'] as String? ?? '',
      birthdate: map['birthdate'] as String? ?? '',
      isPrivate: map['isPrivate'] as bool? ?? false,
      followers: (map['followers'] as num?)?.toInt() ?? 0,
      following: (map['following'] as num?)?.toInt() ?? 0,
      likes: (map['likes'] as num?)?.toInt() ?? 0,
      status: map['status'] as String? ?? 'active',
      role: map['role'] as String? ?? 'user',
    );
  }

  Map<String, dynamic> toMap() => {
        'userId': userId,
        'username': username,
        'avatarUrl': avatarUrl,
        'email': email,
        'phone': phone,
        'birthdate': birthdate,
        'isPrivate': isPrivate,
        'followers': followers,
        'following': following,
        'likes': likes,
        'status': status,
        'role': role,
      };

  UserModel copyWith({
    String? userId,
    String? username,
    String? avatarUrl,
    String? email,
    String? phone,
    String? birthdate,
    bool? isPrivate,
    int? followers,
    int? following,
    int? likes,
    String? status,
    String? role,
  }) {
    return UserModel(
      userId: userId ?? this.userId,
      username: username ?? this.username,
      avatarUrl: avatarUrl ?? this.avatarUrl,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      birthdate: birthdate ?? this.birthdate,
      isPrivate: isPrivate ?? this.isPrivate,
      followers: followers ?? this.followers,
      following: following ?? this.following,
      likes: likes ?? this.likes,
      status: status ?? this.status,
      role: role ?? this.role,
    );
  }
}
