import 'package:firebase_storage/firebase_storage.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import 'package:video_player/video_player.dart';

import 'platform_media_io.dart'
    if (dart.library.html) 'platform_media_web.dart'
    as implementation;

/// Platform-safe operations for media selected through [ImagePicker].
///
/// UI and repositories only exchange [XFile], so browser code never needs to
/// import `dart:io`. The implementation chooses native files on Android/iOS
/// and byte/blob based APIs on the Web.
class PlatformMedia {
  static Future<VideoPlayerController> createVideoPreview(XFile file) {
    return implementation.createVideoPreview(file);
  }

  static Future<http.MultipartFile> createCloudinaryVideoPart(XFile file) {
    return implementation.createCloudinaryVideoPart(file);
  }

  static Future<TaskSnapshot> uploadToFirebaseStorage({
    required Reference reference,
    required XFile file,
    SettableMetadata? metadata,
  }) {
    return implementation.uploadToFirebaseStorage(
      reference: reference,
      file: file,
      metadata: metadata,
    );
  }
}
