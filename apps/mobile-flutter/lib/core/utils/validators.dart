/// Lớp kiểm tra dữ liệu đầu vào
/// Port từ Validator.java trong Android project
class Validators {
  Validators._();

  static final _emailRegex = RegExp(r'^[\w\-.]+@([\w\-]+\.)+[\w\-]{2,}$');
  static final _phoneRegex = RegExp(r'^(0|\+84)\d{9}$');
  static final _usernameRegex = RegExp(r'^[a-zA-Z_][a-zA-Z_0-9]{2,}$');
  static final _dateRegex =
      RegExp(r'^(0?[1-9]|1\d|2\d|3[01])/(0[1-9]|1[0-2])/(\d{4})$');

  /// Kiểm tra email hợp lệ
  static String? validateEmail(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Vui lòng nhập email';
    }
    if (!_emailRegex.hasMatch(value.trim())) {
      return 'Email không hợp lệ';
    }
    return null;
  }

  /// Kiểm tra mật khẩu (tối thiểu 6 ký tự)
  static String? validatePassword(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập mật khẩu';
    }
    if (value.length < 6) {
      return 'Mật khẩu phải chứa ít nhất 6 ký tự';
    }
    return null;
  }

  /// Kiểm tra xác nhận mật khẩu
  static String? validateConfirmPassword(String? value, String password) {
    final error = validatePassword(value);
    if (error != null) return error;
    if (value != password) {
      return 'Mật khẩu xác nhận không khớp';
    }
    return null;
  }

  /// Kiểm tra username hợp lệ
  static String? validateUsername(String? value) {
    if (value == null || value.isEmpty) {
      return 'Vui lòng nhập tên người dùng';
    }
    if (value.length > 50) {
      return 'Tên người dùng không được vượt quá 50 ký tự';
    }
    if (!_usernameRegex.hasMatch(value)) {
      return 'Tên người dùng phải bắt đầu bằng chữ cái hoặc _, tối thiểu 3 ký tự';
    }
    return null;
  }

  /// Kiểm tra số điện thoại (Việt Nam)
  static String? validatePhone(String? value) {
    if (value == null || value.isEmpty) return null; // Không bắt buộc
    if (!_phoneRegex.hasMatch(value)) {
      return 'Số điện thoại không hợp lệ';
    }
    return null;
  }

  /// Kiểm tra ngày sinh (dd/MM/yyyy, tối thiểu 16 tuổi)
  static String? validateBirthdate(String? value) {
    if (value == null || value.isEmpty) return null; // Không bắt buộc
    if (!_dateRegex.hasMatch(value)) {
      return 'Ngày sinh không hợp lệ (dd/MM/yyyy)';
    }
    try {
      final parts = value.split('/');
      final day = int.parse(parts[0]);
      final month = int.parse(parts[1]);
      final year = int.parse(parts[2]);
      final date = DateTime(year, month, day);
      final now = DateTime.now();
      final age = now.year - date.year;
      if (age < 16) {
        return 'Bạn phải đủ 16 tuổi trở lên';
      }
    } catch (_) {
      return 'Ngày sinh không hợp lệ';
    }
    return null;
  }

  /// Kiểm tra chuỗi có phải số hay không
  static bool isNumeric(String? value) {
    if (value == null) return false;
    return double.tryParse(value) != null;
  }
}
