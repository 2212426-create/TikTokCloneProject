import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers/firebase_providers.dart';
import '../models/profile_model.dart';
import '../models/user_model.dart';
import '../repositories/auth_repository.dart';

/// Provider cho AuthRepository
final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepository(
    auth: ref.watch(firebaseAuthProvider),
    firestore: ref.watch(firestoreProvider),
  );
});

/// Provider theo dõi trạng thái đăng nhập (stream)
final authStateProvider = StreamProvider<User?>((ref) {
  return ref.watch(authRepositoryProvider).authStateChanges;
});

/// Provider cho user hiện tại (nullable)
final currentUserProvider = Provider<User?>((ref) {
  return ref.watch(authStateProvider).valueOrNull;
});

/// Provider tải thông tin profile của user hiện tại
final currentUserProfileProvider = FutureProvider<ProfileModel?>((ref) async {
  final user = ref.watch(currentUserProvider);
  if (user == null) return null;
  return ref.watch(authRepositoryProvider).getProfile(user.uid);
});

/// Provider theo dõi trạng thái user trong Firestore (kiểm tra banned)
final userStatusProvider = StreamProvider.family<UserModel?, String>((ref, uid) {
  return ref.watch(authRepositoryProvider).watchUserStatus(uid);
});

/// State cho quá trình đăng nhập/đăng ký
class AuthState {
  final bool isLoading;
  final String? errorMessage;

  const AuthState({
    this.isLoading = false,
    this.errorMessage,
  });

  AuthState copyWith({
    bool? isLoading,
    String? errorMessage,
  }) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}

/// Notifier xử lý các hành động xác thực (đăng nhập, đăng ký, đăng xuất, xóa tài khoản)
class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository _repository;

  AuthNotifier(this._repository) : super(const AuthState());

  /// Đăng ký bằng email
  Future<bool> signUp({
    required String email,
    required String password,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      await _repository.signUpWithEmail(email: email, password: password);
      state = state.copyWith(isLoading: false);
      return true;
    } on FirebaseAuthException catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: _mapFirebaseError(e.code),
      );
      return false;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Đăng ký thất bại: ${e.toString()}',
      );
      return false;
    }
  }

  /// Đăng nhập bằng email
  Future<bool> signIn({
    required String email,
    required String password,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      await _repository.signInWithEmail(email: email, password: password);
      state = state.copyWith(isLoading: false);
      return true;
    } on FirebaseAuthException catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: _mapFirebaseError(e.code),
      );
      return false;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Đăng nhập thất bại: ${e.toString()}',
      );
      return false;
    }
  }

  /// Đăng nhập bằng Google
  Future<bool> signInWithGoogle() async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final user = await _repository.signInWithGoogle();
      state = state.copyWith(isLoading: false);
      return user != null;
    } on FirebaseAuthException catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: _mapFirebaseError(e.code),
      );
      return false;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString().replaceAll('Exception: ', ''),
      );
      return false;
    }
  }

  /// Đăng xuất
  Future<void> signOut() async {
    await _repository.signOut();
  }

  /// Xóa tài khoản
  Future<bool> deleteAccount(String password) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      await _repository.deleteAccount();
      state = state.copyWith(isLoading: false);
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Xóa tài khoản thất bại: ${e.toString()}',
      );
      return false;
    }
  }

  /// Xóa thông báo lỗi
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }

  /// Chuyển mã lỗi Firebase thành thông báo tiếng Việt
  String _mapFirebaseError(String code) {
    switch (code) {
      case 'email-already-in-use':
        return 'Email đã được sử dụng bởi tài khoản khác.';
      case 'invalid-email':
        return 'Email không hợp lệ.';
      case 'weak-password':
        return 'Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn.';
      case 'user-not-found':
        return 'Không tìm thấy tài khoản với email này.';
      case 'wrong-password':
      case 'invalid-credential':
        return 'Email hoặc mật khẩu không đúng.';
      case 'user-disabled':
        return 'Tài khoản đã bị vô hiệu hóa.';
      case 'too-many-requests':
        return 'Quá nhiều yêu cầu. Vui lòng thử lại sau.';
      case 'network-request-failed':
        return 'Lỗi kết nối mạng. Vui lòng kiểm tra internet.';
      default:
        return 'Đã xảy ra lỗi ($code). Vui lòng thử lại.';
    }
  }
}

/// Provider cho AuthNotifier
final authNotifierProvider =
    StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.watch(authRepositoryProvider));
});
