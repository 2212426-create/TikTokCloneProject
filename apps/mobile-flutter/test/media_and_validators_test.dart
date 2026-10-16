import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
import 'package:toptop_flutter/core/utils/validators.dart';

void main() {
  test(
    'XFile keeps byte length without relying on a platform file path',
    () async {
      final media = XFile.fromData(
        Uint8List.fromList([1, 2, 3, 4]),
        name: 'sample.mp4',
        mimeType: 'video/mp4',
      );

      expect(await media.length(), 4);
    },
  );

  test('validators return Vietnamese messages for invalid credentials', () {
    expect(Validators.validateEmail('invalid-email'), 'Email không hợp lệ');
    expect(
      Validators.validatePassword('123'),
      'Mật khẩu phải chứa ít nhất 6 ký tự',
    );
  });
}
