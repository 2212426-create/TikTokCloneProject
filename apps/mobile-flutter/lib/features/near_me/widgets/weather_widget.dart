import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// Service giao diện lấy dữ liệu thời tiết nông nghiệp
class WeatherData {
  final String location;
  final int tempC;
  final String condition;
  final int humidity;
  final String advice;
  final int updatedAt;

  WeatherData({
    required this.location,
    required this.tempC,
    required this.condition,
    required this.humidity,
    required this.advice,
    required this.updatedAt,
  });
}

class WeatherService {
  /// Lấy thời tiết khu vực (Fallback dữ liệu nông nghiệp thực tế khi chưa cấu hình API key)
  static Future<WeatherData> fetchLocalWeather(String region) async {
    await Future.delayed(const Duration(milliseconds: 200));
    final locName = region.isNotEmpty ? region : 'Đức Trọng, Lâm Đồng';
    return WeatherData(
      location: locName,
      tempC: 24,
      condition: 'Nắng nhẹ, thoáng mát',
      humidity: 68,
      advice: 'Thời tiết thuận lợi cho phun xịt & bón phân đợt 2',
      updatedAt: DateTime.now().millisecondsSinceEpoch,
    );
  }
}

/// Compact Weather Card hiển thị trên Video Feed & Tab Gần tôi
class WeatherWidget extends StatelessWidget {
  final String region;

  const WeatherWidget({super.key, this.region = 'Lâm Đồng'});

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<WeatherData>(
      future: WeatherService.fetchLocalWeather(region),
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const SizedBox.shrink();
        final data = snapshot.data!;

        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: Colors.black.withAlpha(120),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.white24, width: 0.5),
          ),
          child: Row(
            children: [
              const Icon(Icons.wb_sunny_outlined, color: AppColors.secondary, size: 24),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      children: [
                        Text(
                          '${data.location} • ${data.tempC}°C',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text(
                          data.condition,
                          style: const TextStyle(color: Colors.white70, fontSize: 11),
                        ),
                      ],
                    ),
                    Text(
                      '💡 ${data.advice}',
                      style: const TextStyle(color: AppColors.secondary, fontSize: 10, fontWeight: FontWeight.w500),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
