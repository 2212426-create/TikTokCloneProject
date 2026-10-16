import 'package:flutter_test/flutter_test.dart';
import 'package:toptop_flutter/features/upload/services/video_copyright_service.dart';

void main() {
  late VideoCopyrightService service;

  setUp(() {
    service = VideoCopyrightService();
  });

  group('Hamming Distance Tests', () {
    test('Identical hashes should have 0 distance', () {
      String hash1 = 'a1c4b789e21045aa';
      String hash2 = 'a1c4b789e21045aa';
      expect(service.computeHammingDistance(hash1, hash2), 0);
    });

    test('Different hashes should have > 0 distance', () {
      String hash1 = '0000000000000000'; // 0 bits
      String hash2 = 'ffffffffffffffff'; // 64 bits (tất cả bit 1)
      expect(service.computeHammingDistance(hash1, hash2), 64);
      
      String hash3 = '0000000000000001'; // Khác 1 bit
      expect(service.computeHammingDistance(hash1, hash3), 1);
    });
  });

  group('Visual Similarity Tests', () {
    test('Identical keyframes should return 100% similarity (1.0)', () {
      List<String> hashes1 = ['a1c4', 'b8e2', 'c4b7'];
      List<String> hashes2 = ['a1c4', 'b8e2', 'c4b7'];
      expect(service.computeVisualSimilarity(hashes1, hashes2), 1.0);
    });

    test('Completely different keyframes should return 0% similarity (0.0)', () {
      List<String> hashes1 = ['0000000000000000'];
      List<String> hashes2 = ['ffffffffffffffff'];
      expect(service.computeVisualSimilarity(hashes1, hashes2), 0.0);
    });
  });

  group('Decision Matrix Tests', () {
    test('Similarity < 70% -> Approved', () {
      final eval = service.evaluateCopyrightRisk(
        visualSimilarity: 0.50, // 50%
        audioSimilarity: 0.40,  // 40%
        isSameAuthor: false,
      );
      // Risk = 0.75 * 0.5 + 0.25 * 0.4 = 0.375 + 0.1 = 0.475 (< 0.70)
      expect(eval.decision, CopyrightDecision.approved);
      expect(eval.riskScore, closeTo(0.475, 0.001));
    });

    test('Similarity >= 90% (Khác tác giả) -> Blocked', () {
      final eval = service.evaluateCopyrightRisk(
        visualSimilarity: 0.95, 
        audioSimilarity: 0.90,  
        isSameAuthor: false,
      );
      // Risk = 0.75*0.95 + 0.25*0.90 = 0.7125 + 0.225 = 0.9375 (>= 0.90)
      expect(eval.decision, CopyrightDecision.blocked);
    });

    test('Similarity 70% - 89% (Khác tác giả) -> Pending Review', () {
      final eval = service.evaluateCopyrightRisk(
        visualSimilarity: 0.85, 
        audioSimilarity: 0.80,  
        isSameAuthor: false,
      );
      // Risk = 0.75*0.85 + 0.25*0.80 = 0.6375 + 0.2 = 0.8375 (>=0.70, <0.90)
      expect(eval.decision, CopyrightDecision.pendingReview);
    });

    test('Similarity >= 70% (Cùng tác giả) -> Duplicate (Nhắc nhở)', () {
      final eval = service.evaluateCopyrightRisk(
        visualSimilarity: 0.99, 
        audioSimilarity: 0.95,  
        isSameAuthor: true,
      );
      expect(eval.decision, CopyrightDecision.duplicate);
    });
  });

  group('Database Matching Simulation', () {
    test('Find most similar video from Firestore mock data', () async {
      List<Map<String, dynamic>> dbRecords = [
        {
          'video_id': 'vid_1',
          'author_id': 'user_a',
          'keyframes_phash': ['0000', '0000', '0000', '0000', '0000'] // Khác hoàn toàn
        },
        {
          'video_id': 'vid_2',
          'author_id': 'user_b',
          'keyframes_phash': ['a1c4', 'b8e2', 'c4b7', 'e210', '1045'] // Giống
        },
      ];

      List<String> newVideoHashes = ['a1c4', 'b8e2', 'c4b7', 'e210', '1040']; // Sai 1 bit nhỏ ở cuối

      var bestMatch = await service.findMostSimilarVideo(
        newHashes: newVideoHashes, 
        databaseRecords: dbRecords,
      );

      expect(bestMatch, isNotNull);
      expect(bestMatch!['video_id'], 'vid_2');
      expect(bestMatch['calculated_similarity'], greaterThan(0.95)); // Rất giống
    });
  });
}
