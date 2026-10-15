import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../auth/providers/auth_provider.dart';
import '../repositories/quote_repository.dart';
import '../widgets/quote_card_widget.dart';

/// Màn hình Quản lý Yêu Cầu Báo Giá (Cho cả Người mua & Người bán)
class QuoteRequestsScreen extends ConsumerWidget {
  const QuoteRequestsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(currentUserProvider);

    if (currentUser == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Yêu cầu báo giá')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.sell_outlined, size: 64, color: AppColors.primary),
              const SizedBox(height: 16),
              const Text('Vui lòng đăng nhập để xem các báo giá!'),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => context.go('/auth'),
                child: const Text('Đăng nhập'),
              ),
            ],
          ),
        ),
      );
    }

    final quotesAsync = ref.watch(userQuotesStreamProvider(currentUser.uid));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Yêu Cầu Báo Giá'),
      ),
      body: quotesAsync.when(
        data: (quotes) {
          if (quotes.isEmpty) {
            return const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.sell_outlined, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('Chưa có yêu cầu báo giá nào.'),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(12),
            itemCount: quotes.length,
            itemBuilder: (context, index) {
              final quote = quotes[index];
              return QuoteCardWidget(quote: quote);
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Lỗi: $err')),
      ),
    );
  }
}
