import 'dart:typed_data';
import 'package:google_generative_ai/google_generative_ai.dart';
import '../../../core/constants/app_constants.dart';

/// Dịch vụ kết nối và gọi mô hình Gemini AI để phân tích hình ảnh đề xuất hashtag
class GeminiService {
  final GenerativeModel _model;

  GeminiService()
      : _model = GenerativeModel(
          model: 'gemini-1.5-flash',
          apiKey: AppConstants.geminiApiKey,
          generationConfig: GenerationConfig(
            temperature: 0.7,
            topK: 40,
            topP: 0.95,
            maxOutputTokens: 150,
          ),
        );

  /// Gửi ảnh dưới dạng byte lên Gemini để phân tích nhận diện và gợi ý các hashtag viral tiếng Việt
  Future<String> suggestHashtags(Uint8List imageBytes) async {
    const prompt = 'Bạn là một chuyên gia TikTok Marketing Việt Nam. '
        'Hãy nhìn hình ảnh này và gợi ý 5-10 hashtag TikTok tiếng Việt cực kỳ viral và liên quan nhất. '
        'YÊU CẦU: Mỗi hashtag phải bắt đầu bằng dấu #. Chỉ trả về danh sách hashtag cách nhau bởi dấu cách. '
        'Không thêm bất kỳ câu chào hay lời giải thích nào.';

    final content = [
      Content.multi([
        TextPart(prompt),
        DataPart('image/jpeg', imageBytes),
      ]),
    ];

    final response = await _model.generateContent(content);
    return response.text ?? '';
  }

  /// MOCK AI (Dành cho Thành viên 1 / 3): Giả lập AI phân tích video
  /// Nhận diện nội dung Nông nghiệp & Kiểm duyệt an toàn mà không tốn API
  Future<Map<String, dynamic>> mockAnalyzeVideo() async {
    await Future.delayed(const Duration(seconds: 1)); // Giả lập độ trễ mạng
    return {
      'is_agriculture': true,
      'safety_score': 0.98,
      'suggested_hashtags': ['#nongnghiep', '#thiennhien', '#toptop'],
      'category': 'Agriculture',
    };
  }
}
