import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/providers/theme_provider.dart';

/// Màn hình Cài đặt Giao diện (Light / Dark Mode / System)
class AppearanceScreen extends ConsumerWidget {
  const AppearanceScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentThemeMode = ref.watch(themeNotifierProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Giao diện hiển thị'),
      ),
      body: ListView(
        children: [
          const Padding(
            padding: EdgeInsets.all(16.0),
            child: Text(
              'Chọn giao diện hiển thị cho ứng dụng ViFarm',
              style: TextStyle(color: Colors.grey, fontSize: 13),
            ),
          ),
          RadioListTile<ThemeMode>(
            title: const Text('Tự động theo hệ thống'),
            subtitle: const Text('Sử dụng cài đặt sáng/tối của thiết bị'),
            value: ThemeMode.system,
            groupValue: currentThemeMode,
            onChanged: (mode) {
              if (mode != null) {
                ref.read(themeNotifierProvider.notifier).setThemeMode(mode);
              }
            },
          ),
          RadioListTile<ThemeMode>(
            title: const Text('Giao diện Sáng (Light Mode)'),
            value: ThemeMode.light,
            groupValue: currentThemeMode,
            onChanged: (mode) {
              if (mode != null) {
                ref.read(themeNotifierProvider.notifier).setThemeMode(mode);
              }
            },
          ),
          RadioListTile<ThemeMode>(
            title: const Text('Giao diện Tối (Dark Mode)'),
            value: ThemeMode.dark,
            groupValue: currentThemeMode,
            onChanged: (mode) {
              if (mode != null) {
                ref.read(themeNotifierProvider.notifier).setThemeMode(mode);
              }
            },
          ),
        ],
      ),
    );
  }
}
