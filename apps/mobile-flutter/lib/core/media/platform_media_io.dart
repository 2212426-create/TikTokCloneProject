import 'dart:io';

import 'package:firebase_storage/firebase_storage.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import 'package:video_player/video_player.dart';

Future<VideoPlayerController> createVideoPreview(XFile file) async {
  return VideoPlayerController.file(File(file.path));
}

Future<http.MultipartFile> createCloudinaryVideoPart(XFile file) {
  return http.MultipartFile.fromPath('file', file.path, filename: file.name);
}

Future<TaskSnapshot> uploadToFirebaseStorage({
  required Reference reference,
  required XFile file,
  SettableMetadata? metadata,
}) async {
  return reference.putData(await file.readAsBytes(), metadata);
}
