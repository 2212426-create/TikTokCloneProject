package com.example.tiktokcloneproject.helper;

import android.graphics.Bitmap;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

public class GeminiHelper {
    // CHÚ Ý: Dán API Key của bạn vào đây. Lấy tại: https://aistudio.google.com/app/apikey
    private static final String GEMINI_API_KEY = "AIzaSyCw3Z3vGilpO7eUg-gLNDO1JunJFjhq7XM";

    public static ListenableFuture<GenerateContentResponse> suggestHashtags(Bitmap bitmap) {
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-pro",
                GEMINI_API_KEY
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addText("Dựa vào hình ảnh từ video này, hãy gợi ý 5-10 hashtag TikTok tiếng Việt đang thịnh hành (viral) và liên quan nhất. " +
                        "YÊU CẦU: Mỗi hashtag phải bắt đầu bằng dấu #. Chỉ trả về các hashtag, cách nhau bởi dấu cách, không thêm bất kỳ lời dẫn nào.")
                .addImage(bitmap)
                .build();

        return model.generateContent(content);
    }
}
