/// Utility class cho xử lý chuỗi tiếng Việt và tìm kiếm
class TextUtils {
  TextUtils._();

  /// Chuẩn hóa từ khóa: xóa dấu tiếng Việt, chuyển chữ thường, xóa khoảng trắng thừa
  static String removeDiacritics(String str) {
    if (str.isEmpty) return '';

    var result = str.trim().toLowerCase();

    const vietnamese = [
      'aáàảãạăắằẳẵặâấầẩẫậ',
      'AÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬ',
      'dđ',
      'DĐ',
      'eéèẻẽẹêếềểễệ',
      'EÉÈẺẼẸÊẾỀỂỄỆ',
      'iíìỉĩị',
      'IÍÌỈĨỊ',
      'oóòỏõọôốồổỗộơớờởỡợ',
      'OÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢ',
      'uúùủũụưứừửữự',
      'UÚÙỦŨỤƯỨỪỬỮỰ',
      'yýỳỷỹỵ',
      'YÝỲỶỸỴ',
    ];

    for (var element in vietnamese) {
      final source = element.substring(1);
      final target = element[0];
      for (var i = 0; i < source.length; i++) {
        result = result.replaceAll(source[i], target);
      }
    }

    return result.replaceAll(RegExp(r'\s+'), ' ');
  }

  /// Chuẩn hóa hashtag (bỏ dấu #, xóa khoảng trắng, chữ thường)
  static String normalizeHashtag(String tag) {
    final clean = tag.trim().replaceAll('#', '');
    return removeDiacritics(clean);
  }
}
