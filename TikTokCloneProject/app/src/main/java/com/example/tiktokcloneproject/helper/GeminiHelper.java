package com.example.tiktokcloneproject.helper;

import android.graphics.Bitmap;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.ListenableFuture;

public class GeminiHelper {
    // API Key của Gemini
    private static final String GEMINI_API_KEY = "AIzaSyCw3Z3vGilpO7eUg-gLNDO1JunJFjhq7XM";

    public static ListenableFuture<GenerateContentResponse> suggestHashtags(Bitmap bitmap) {
        // Cấu hình tham số cho model Pro để có kết quả tốt nhất và phản hồi nhanh
        // Trong Java SDK, GenerationConfig.Builder sử dụng truy cập field trực tiếp thay vì các phương thức setter
        GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
        configBuilder.temperature = 0.7f;
        configBuilder.topK = 40;
        configBuilder.topP = 0.95f;
        configBuilder.maxOutputTokens = 150;
        GenerationConfig config = configBuilder.build();

        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-pro", 
                GEMINI_API_KEY,
                config
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Câu lệnh yêu cầu AI phân tích ảnh và đưa ra hashtag viral
        Content content = new Content.Builder()
                .addText("Bạn là một chuyên gia TikTok Marketing Việt Nam. " +
                        "Hãy nhìn hình ảnh này và gợi ý 5-10 hashtag TikTok tiếng Việt cực kỳ viral và liên quan nhất. " +
                        "YÊU CẦU: Mỗi hashtag phải bắt đầu bằng dấu #. Chỉ trả về danh sách hashtag cách nhau bởi dấu cách. " +
                        "Không thêm bất kỳ câu chào hay lời giải thích nào.")
                .addImage(bitmap)
                .build();

        return model.generateContent(content);
    }
}
