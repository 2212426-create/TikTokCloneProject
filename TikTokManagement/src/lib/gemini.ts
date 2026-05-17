const GEMINI_API_KEY = import.meta.env.VITE_GEMINI_API_KEY;
const GEMINI_API_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${GEMINI_API_KEY}`;

export interface AIModerationResult {
  isViolation: boolean;
  confidence: number; // 0-100
  category: string;
  reason: string;
  details: string;
}

/**
 * Sử dụng Gemini AI để kiểm duyệt nội dung mô tả video
 * có vi phạm tiêu chuẩn cộng đồng hay không.
 */
export async function moderateVideoContent(
  description: string,
  username: string,
): Promise<AIModerationResult> {
  if (!GEMINI_API_KEY) {
    throw new Error('VITE_GEMINI_API_KEY chưa được cấu hình');
  }

  const prompt = `Bạn là hệ thống kiểm duyệt nội dung của nền tảng video ngắn (tương tự TikTok). 
Hãy phân tích mô tả video sau và đánh giá xem có vi phạm tiêu chuẩn cộng đồng không.

Tiêu chuẩn cộng đồng bao gồm:
1. Không nội dung bạo lực, đe dọa, tự gây hại
2. Không nội dung khiêu dâm, khỏa thân, gợi dục
3. Không spam, lừa đảo, quảng cáo sai sự thật
4. Không phân biệt đối xử, phát ngôn thù ghét
5. Không xâm phạm quyền riêng tư
6. Không nội dung vi phạm bản quyền
7. Không quảng bá ma túy, chất cấm, vũ khí
8. Không thông tin sai lệch nguy hiểm

Thông tin video:
- Người đăng: @${username}
- Mô tả: "${description}"

Trả lời theo định dạng JSON (không có markdown block):
{
  "isViolation": true/false,
  "confidence": <số từ 0-100>,
  "category": "<loại vi phạm hoặc 'none'>",
  "reason": "<lý do ngắn gọn bằng tiếng Việt>",
  "details": "<phân tích chi tiết bằng tiếng Việt>"
}`;

  try {
    const response = await fetch(GEMINI_API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: {
          temperature: 0.1,
          maxOutputTokens: 512,
        },
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Gemini API error: ${response.status} - ${error}`);
    }

    const data = await response.json();
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text || '';

    // Parse JSON from response (remove potential markdown code blocks)
    const jsonStr = text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
    const result = JSON.parse(jsonStr);

    return {
      isViolation: result.isViolation || false,
      confidence: Math.min(100, Math.max(0, result.confidence || 0)),
      category: result.category || 'none',
      reason: result.reason || '',
      details: result.details || '',
    };
  } catch (err: any) {
    console.error('AI Moderation error:', err);
    // Return safe default on error
    return {
      isViolation: false,
      confidence: 0,
      category: 'error',
      reason: 'Không thể phân tích: ' + (err.message || 'Lỗi không xác định'),
      details: 'Hệ thống AI gặp lỗi, cần kiểm duyệt thủ công.',
    };
  }
}

/**
 * Quét hàng loạt video và trả về danh sách vi phạm
 */
export async function batchModerateVideos(
  videos: Array<{ videoId: string; description: string; username: string }>
): Promise<Map<string, AIModerationResult>> {
  const results = new Map<string, AIModerationResult>();

  // Process in batches of 3 to avoid rate limiting
  for (let i = 0; i < videos.length; i += 3) {
    const batch = videos.slice(i, i + 3);
    const promises = batch.map(async (v) => {
      const result = await moderateVideoContent(v.description, v.username);
      results.set(v.videoId, result);
    });
    await Promise.all(promises);
    // Small delay between batches
    if (i + 3 < videos.length) {
      await new Promise((r) => setTimeout(r, 500));
    }
  }

  return results;
}
