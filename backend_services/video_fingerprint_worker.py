import cv2
import numpy as np
import hashlib
import os

def get_video_duration(video_path):
    """Lấy thời lượng video bằng OpenCV"""
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        return 0
    fps = cap.get(cv2.CAP_PROP_FPS)
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = frame_count / fps if fps > 0 else 0
    cap.release()
    return duration

def extract_keyframes(video_path, num_frames=5):
    """
    Trích xuất N keyframes tại các mốc % thời gian cố định 
    để tạo fingerprint chống lại việc cắt xén nhẹ.
    (Ví dụ: 10%, 30%, 50%, 70%, 90%)
    """
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise Exception(f"Không thể mở video {video_path}")
        
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    
    # Tính toán các vị trí frame cần lấy
    percentages = [0.1, 0.3, 0.5, 0.7, 0.9]
    frame_indices = [int(frame_count * p) for p in percentages]
    
    keyframes = []
    
    for idx in frame_indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
        ret, frame = cap.read()
        if ret:
            keyframes.append(frame)
        else:
            # Fallback nếu video bị lỗi frames cuối
            keyframes.append(np.zeros((32, 32, 3), dtype=np.uint8))
            
    cap.release()
    return keyframes

def compute_phash(image, hash_size=8):
    """
    Tự cài đặt thuật toán Perceptual Hash (pHash) dựa trên DCT.
    - Resize ảnh về 32x32
    - Chuyển sang ảnh xám
    - Tính ma trận DCT (Discrete Cosine Transform)
    - Lấy vùng tần số thấp 8x8 (loại bỏ chi tiết nhiễu)
    - So sánh với giá trị trung vị
    """
    # Resize và chuyển sang ảnh xám (Grayscale)
    img_resized = cv2.resize(image, (32, 32), interpolation=cv2.INTER_AREA)
    gray = cv2.cvtColor(img_resized, cv2.COLOR_BGR2GRAY)
    
    # Tính DCT (Sử dụng float32 theo yêu cầu của OpenCV DCT)
    dct = cv2.dct(np.float32(gray))
    
    # Lấy vùng tần số thấp (top-left 8x8)
    dctlowfreq = dct[0:hash_size, 0:hash_size]
    
    # Lấy giá trị trung vị (loại bỏ phần tử đầu tiên dctlowfreq[0,0] thường rất lớn vì nó đại diện cho độ sáng trung bình)
    med = np.median(dctlowfreq[1:])
    
    # Tạo chuỗi hash
    hash_matrix = dctlowfreq > med
    
    # Chuyển ma trận True/False thành chuỗi Hex 64-bit
    hash_bits = hash_matrix.flatten()
    hash_hex = ""
    for i in range(0, len(hash_bits), 4):
        # Lấy 4 bit mỗi lần để tạo 1 ký tự hex
        bit_val = 0
        for j in range(4):
            if i+j < len(hash_bits) and hash_bits[i+j]:
                bit_val += (1 << (3 - j))
        hash_hex += hex(bit_val)[2:]
        
    return hash_hex

def generate_video_dna(video_path):
    """
    Tạo Video DNA tổng hợp cho một video file.
    Trả về Dict cấu trúc giống Firestore Schema đã thiết kế.
    """
    try:
        duration = get_video_duration(video_path)
        keyframes = extract_keyframes(video_path, num_frames=5)
        
        hashes = [compute_phash(frame) for frame in keyframes]
        
        # Sinh audio hash giả lập (Thực tế sẽ dùng thư viện Librosa hoặc Chromaprint)
        # Ở đây mock bằng SHA-256 của file size để minh họa
        file_size = os.path.getsize(video_path)
        audio_mock_hash = hashlib.sha256(str(file_size).encode()).hexdigest()[:32]
        
        return {
            "duration_seconds": round(duration, 2),
            "keyframes_phash": hashes,
            "audio_signature_hash": audio_mock_hash
        }
    except Exception as e:
        print(f"Lỗi khi tạo DNA: {e}")
        return None

if __name__ == "__main__":
    print("🚀 Khởi chạy Video Fingerprint Worker (Người 2)")
    print("Đang chờ video đầu vào...")
    # Mô phỏng test (cần 1 file video thực tế để chạy)
    # dna = generate_video_dna("sample_video.mp4")
    # print(f"🧬 Kết quả Video DNA: {dna}")
