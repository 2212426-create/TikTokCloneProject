import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/time_utils.dart';
import '../../auth/providers/auth_provider.dart';
import '../models/journal_model.dart';
import '../repositories/journal_repository.dart';

/// Màn hình Chi tiết Nhật ký mùa vụ với Timeline sinh động
class JournalDetailScreen extends ConsumerWidget {
  final String journalId;

  const JournalDetailScreen({
    super.key,
    required this.journalId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(currentUserProvider);
    final journalAsync = ref.watch(journalDetailStreamProvider(journalId));
    final entriesAsync = ref.watch(journalEntriesStreamProvider(journalId));
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return journalAsync.when(
      data: (journal) {
        if (journal == null) {
          return Scaffold(
            appBar: AppBar(title: const Text('Nhật ký mùa vụ')),
            body: const Center(child: Text('Không tìm thấy nhật ký này!')),
          );
        }

        final isOwner = currentUser != null && currentUser.uid == journal.ownerId;
        final isPrivate = journal.privacy == 'private' && !isOwner;

        if (isPrivate) {
          return Scaffold(
            appBar: AppBar(title: const Text('Nhật ký mùa vụ')),
            body: const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.lock, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text('Nhật ký này ở chế độ riêng tư.'),
                ],
              ),
            ),
          );
        }

        final startDate = DateTime.fromMillisecondsSinceEpoch(journal.startDate);

        return Scaffold(
          appBar: AppBar(
            title: Text(journal.name),
            actions: [
              if (isOwner)
                IconButton(
                  icon: const Icon(Icons.add_task),
                  tooltip: 'Thêm mục nhật ký',
                  onPressed: () {
                    context.push('/journal/${journal.id}/entry/create');
                  },
                ),
            ],
          ),
          body: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header card
                Card(
                  elevation: 2,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: AppColors.primary.withAlpha(40),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Text(
                                '🌱 ${journal.crop} ${journal.variety != null ? "(${journal.variety})" : ""}',
                                style: const TextStyle(
                                  color: AppColors.primary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                            const Spacer(),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                              decoration: BoxDecoration(
                                color: journal.status == 'in_progress'
                                    ? Colors.green.withAlpha(40)
                                    : Colors.blue.withAlpha(40),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                journal.status == 'in_progress' ? 'Đang canh tác' : 'Đã kết thúc',
                                style: TextStyle(
                                  color: journal.status == 'in_progress' ? Colors.green : Colors.blue,
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Text(
                          journal.name,
                          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            const Icon(Icons.location_on, size: 16, color: Colors.grey),
                            const SizedBox(width: 4),
                            Text(
                              '${journal.farmLocation} - ${journal.region}',
                              style: const TextStyle(color: Colors.grey, fontSize: 13),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            const Icon(Icons.calendar_today, size: 16, color: Colors.grey),
                            const SizedBox(width: 4),
                            Text(
                              'Bắt đầu: ${startDate.day}/${startDate.month}/${startDate.year}',
                              style: const TextStyle(color: Colors.grey, fontSize: 13),
                            ),
                          ],
                        ),
                        if (journal.description.isNotEmpty) ...[
                          const SizedBox(height: 10),
                          const Divider(),
                          Text(
                            journal.description,
                            style: const TextStyle(fontSize: 13, height: 1.4),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 20),

                // Timeline Entries Header
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Dòng thời gian nhật ký',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.primary),
                    ),
                    if (isOwner)
                      TextButton.icon(
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Đăng mục mới'),
                        onPressed: () {
                          context.push('/journal/${journal.id}/entry/create');
                        },
                      ),
                  ],
                ),
                const SizedBox(height: 12),

                // Timeline entries
                entriesAsync.when(
                  data: (entries) {
                    if (entries.isEmpty) {
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(32),
                          child: Column(
                            children: [
                              Icon(
                                Icons.note_add_outlined,
                                size: 48,
                                color: isDark ? AppColors.darkTextHint : AppColors.lightTextHint,
                              ),
                              const SizedBox(height: 12),
                              const Text('Chưa có ghi chép nào trong nhật ký này.'),
                              if (isOwner) const Text('Bấm "Đăng mục mới" để bắt đầu ghi chép!'),
                            ],
                          ),
                        ),
                      );
                    }

                    return ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: entries.length,
                      itemBuilder: (context, index) {
                        final entry = entries[index];
                        final recordDate = DateTime.fromMillisecondsSinceEpoch(entry.recordDate);
                        final isLast = index == entries.length - 1;

                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Left timeline node
                            Column(
                              children: [
                                Container(
                                  width: 16,
                                  height: 16,
                                  decoration: BoxDecoration(
                                    color: AppColors.primary,
                                    shape: BoxShape.circle,
                                    border: Border.all(color: Colors.white, width: 2),
                                  ),
                                ),
                                if (!isLast)
                                  Container(
                                    width: 2,
                                    height: 120,
                                    color: AppColors.primary.withAlpha(80),
                                  ),
                              ],
                            ),
                            const SizedBox(width: 12),

                            // Right entry content card
                            Expanded(
                              child: Card(
                                margin: const EdgeInsets.only(bottom: 16),
                                child: Padding(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                        children: [
                                          Text(
                                            '${recordDate.day}/${recordDate.month}/${recordDate.year}',
                                            style: const TextStyle(
                                              fontWeight: FontWeight.bold,
                                              color: AppColors.primary,
                                              fontSize: 13,
                                            ),
                                          ),
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                            decoration: BoxDecoration(
                                              color: AppColors.accentBrown.withAlpha(30),
                                              borderRadius: BorderRadius.circular(4),
                                            ),
                                            child: Text(
                                              entry.growthStage,
                                              style: const TextStyle(
                                                color: AppColors.accentBrown,
                                                fontSize: 10,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        entry.title,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 15,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        entry.notes,
                                        style: const TextStyle(fontSize: 13, height: 1.4),
                                      ),
                                      if (entry.attachedVideoUrl != null && entry.attachedVideoUrl!.isNotEmpty) ...[
                                        const SizedBox(height: 8),
                                        OutlinedButton.icon(
                                          icon: const Icon(Icons.play_circle_fill, size: 18),
                                          label: const Text('Xem video kỹ thuật'),
                                          onPressed: () {
                                            context.push('/video/${entry.attachedVideoUrl}');
                                          },
                                        ),
                                      ],
                                    ],
                                  ),
                                ),
                              ),
                            ),
                          ],
                        );
                      },
                    );
                  },
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (err, stack) => Text('Lỗi: $err'),
                ),
              ],
            ),
          ),
        );
      },
      loading: () => const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (err, stack) => Scaffold(body: Center(child: Text('Lỗi: $err'))),
    );
  }
}
