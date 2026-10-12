import 'dart:math';

/// Phân loại quyết định bản quyền
enum CopyrightDecision {
  approved,       // Hợp lệ, xuất bản
  duplicate,      // Cùng tác giả re-upload
  pendingReview,  // Nghi vấn, đẩy lên Web Admin
  blocked,        // Chặn tức thì, vi phạm bản quyền
}

/// Kết quả đánh giá bản quyền tổng hợp
class CopyrightEvaluation {
  final CopyrightDecision decision;
  final double riskScore;
  final double visualSimilarity;
  final double audioSimilarity;
  final String message;

  CopyrightEvaluation({
    required this.decision,
    required this.riskScore,
    required this.visualSimilarity,
    required this.audioSimilarity,
    required this.message,
  });

  @override
  String toString() {
    return 'Decision: ${decision.name.toUpperCase()} | Risk: ${(riskScore * 100).toStringAsFixed(1)}% | Msg: $message';
  }
}

class VideoCopyrightService {
  /// 1. Tính khoảng cách Hamming giữa 2 chuỗi Hex Hash (ví dụ pHash 64-bit)
  int computeHammingDistance(String hash1, String hash2) {
    if (hash1.length != hash2.length) return 64; // Fallback
    
    int distance = 0;
    try {
      // Chuyển hex string sang chuỗi nhị phân để đếm bit khác nhau
      for (int i = 0; i < hash1.length; i++) {
        int val1 = int.parse(hash1[i], radix: 16);
        int val2 = int.parse(hash2[i], radix: 16);
        int xor = val1 ^ val2;
        // Đếm số bit 1 trong kết quả XOR
        while (xor > 0) {
          distance += xor & 1;
          xor >>= 1;
        }
      }
    } catch (e) {
      return 64;
    }
    return distance;
  }

  /// 2. Tính tỷ lệ tương đồng hình ảnh (Visual Similarity) dựa trên mảng 5 Keyframe pHash
  double computeVisualSimilarity(List<String> sourceHashes, List<String> targetHashes) {
    if (sourceHashes.isEmpty || targetHashes.isEmpty) return 0.0;
    
    int totalDistance = 0;
    int framesToCompare = min(sourceHashes.length, targetHashes.length);
    
    for (int i = 0; i < framesToCompare; i++) {
      totalDistance += computeHammingDistance(sourceHashes[i], targetHashes[i]);
    }
    
    // Mỗi hash 64-bit -> tối đa 64 khoảng cách cho mỗi frame
    int maxPossibleDistance = framesToCompare * 64;
    double similarity = 1.0 - (totalDistance / maxPossibleDistance);
    return max(0.0, similarity);
  }

  /// 3. So khớp Video Mới với CSDL để tìm Video có độ tương đồng cao nhất
  /// Trong thực tế, dữ liệu này gọi từ Firestore/Vector DB
  Future<Map<String, dynamic>?> findMostSimilarVideo({
    required List<String> newHashes,
    required List<Map<String, dynamic>> databaseRecords,
  }) async {
    double highestSimilarity = 0.0;
    Map<String, dynamic>? bestMatch;

    for (var record in databaseRecords) {
      List<String> dbHashes = List<String>.from(record['keyframes_phash'] ?? []);
      double sim = computeVisualSimilarity(newHashes, dbHashes);
      
      if (sim > highestSimilarity) {
        highestSimilarity = sim;
        bestMatch = record;
        bestMatch['calculated_similarity'] = sim;
      }
    }

    return bestMatch;
  }

  /// 4. Đánh giá rủi ro Bản Quyền & Ra Quyết Định (Decision Matrix)
  CopyrightEvaluation evaluateCopyrightRisk({
    required double visualSimilarity,
    required double audioSimilarity, // Giả lập tỷ lệ trùng âm thanh
    required bool isSameAuthor,
  }) {
    // Trọng số: 75% Hình ảnh + 25% Âm thanh
    double riskScore = (0.75 * visualSimilarity) + (0.25 * audioSimilarity);

    if (isSameAuthor && riskScore >= 0.70) {
      return CopyrightEvaluation(
        decision: CopyrightDecision.duplicate,
        riskScore: riskScore,
        visualSimilarity: visualSimilarity,
        audioSimilarity: audioSimilarity,
        message: 'Video đã tồn tại trên kênh của bạn. Cho phép tiếp tục nhưng cảnh báo trùng lặp.',
      );
    }

    if (riskScore >= 0.90) {
      return CopyrightEvaluation(
        decision: CopyrightDecision.blocked,
        riskScore: riskScore,
        visualSimilarity: visualSimilarity,
        audioSimilarity: audioSimilarity,
        message: '⛔ TỪ CHỐI TẢI LÊN. Video vi phạm bản quyền (Re-upload > 90%).',
      );
    }

    if (riskScore >= 0.70) {
      return CopyrightEvaluation(
        decision: CopyrightDecision.pendingReview,
        riskScore: riskScore,
        visualSimilarity: visualSimilarity,
        audioSimilarity: audioSimilarity,
        message: '⚠️ GẮN CỜ NGHI VẤN. Video có dấu hiệu cắt ghép/lách bản quyền. Đẩy vào Web Admin để duyệt thủ công.',
      );
    }

    return CopyrightEvaluation(
      decision: CopyrightDecision.approved,
      riskScore: riskScore,
      visualSimilarity: visualSimilarity,
      audioSimilarity: audioSimilarity,
      message: '✅ HỢP LỆ. Cấp phép xuất bản và lưu DNA mới vào cơ sở dữ liệu.',
    );
  }
}
