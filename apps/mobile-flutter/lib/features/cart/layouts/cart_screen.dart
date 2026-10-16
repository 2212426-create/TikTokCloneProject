import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../auth/providers/auth_provider.dart';
import '../models/cart_model.dart';
import '../providers/cart_provider.dart';

/// Màn hình Giỏ Hàng Nông Nghiệp
class CartScreen extends ConsumerWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(currentUserProvider);
    final cartItems = ref.watch(cartProvider);
    final cartGroups = ref.watch(cartGroupsProvider);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    if (currentUser == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('Giỏ hàng')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.shopping_cart_outlined, size: 64, color: AppColors.primary),
              const SizedBox(height: 16),
              const Text('Vui lòng đăng nhập để xem giỏ hàng!'),
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

    if (cartItems.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: const Text('Giỏ hàng nông sản')),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.remove_shopping_cart_outlined,
                size: 72,
                color: isDark ? AppColors.darkTextHint : AppColors.lightTextHint,
              ),
              const SizedBox(height: 16),
              const Text(
                'Giỏ hàng của bạn đang trống!',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: () => context.go('/home'),
                child: const Text('Khám phá sản phẩm'),
              ),
            ],
          ),
        ),
      );
    }

    // Grand total calculation for selected fixed_price items
    final grandTotal = cartGroups.fold<int>(
      0,
      (sum, group) => sum + group.selectedFixedPriceTotal,
    );

    final selectedItemsCount = cartItems.where((i) => i.isSelected && i.saleType == 'fixed_price').length;

    return Scaffold(
      appBar: AppBar(
        title: Text('Giỏ hàng (${cartItems.length})'),
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: cartGroups.length,
              itemBuilder: (context, groupIndex) {
                final group = cartGroups[groupIndex];
                return _CartGroupCard(group: group);
              },
            ),
          ),

          // Bottom Bar (Checkout Total)
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: isDark ? AppColors.darkSurface : AppColors.lightSurface,
              boxShadow: const [
                BoxShadow(color: Colors.black12, blurRadius: 4, offset: Offset(0, -2)),
              ],
            ),
            child: Row(
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text('Tổng thanh toán (Giá cố định):', style: TextStyle(fontSize: 12, color: Colors.grey)),
                    const SizedBox(height: 2),
                    Text(
                      _formatVND(grandTotal),
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const Spacer(),

                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                  ),
                  onPressed: selectedItemsCount > 0
                      ? () {
                          context.push('/checkout');
                        }
                      : null,
                  child: Text(
                    'Đặt Hàng ($selectedItemsCount)',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _formatVND(int price) {
    if (price <= 0) return '0đ';
    return '${price.toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (m) => '.')}đ';
  }
}

class _CartGroupCard extends ConsumerWidget {
  final CartGroupModel group;

  const _CartGroupCard({required this.group});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Seller Header with Checkbox
            Row(
              children: [
                Checkbox(
                  value: group.isAllSelected,
                  activeColor: AppColors.primary,
                  onChanged: (val) {
                    if (val != null) {
                      ref.read(cartProvider.notifier).toggleGroupSelection(group.sellerId, val);
                    }
                  },
                ),
                const Icon(Icons.store, size: 18, color: AppColors.primary),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    group.sellerName,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                ),
              ],
            ),
            const Divider(),

            // Items List
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: group.items.length,
              separatorBuilder: (context, index) => const Divider(height: 12),
              itemBuilder: (context, index) {
                final item = group.items[index];
                return _CartItemTile(item: item);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _CartItemTile extends ConsumerWidget {
  final CartItemModel item;

  const _CartItemTile({required this.item});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isFixed = item.saleType == 'fixed_price';

    return Row(
      children: [
        Checkbox(
          value: item.isSelected,
          activeColor: AppColors.primary,
          onChanged: (val) {
            ref.read(cartProvider.notifier).toggleItemSelection(item.cartItemId);
          },
        ),

        // Thumbnail
        Container(
          width: 60,
          height: 60,
          decoration: BoxDecoration(
            color: AppColors.primary.withAlpha(20),
            borderRadius: BorderRadius.circular(6),
          ),
          child: item.productImage.isNotEmpty
              ? Image.network(
                  item.productImage,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) =>
                      const Icon(Icons.image, color: Colors.grey),
                )
              : const Icon(Icons.grass, color: AppColors.primary),
        ),
        const SizedBox(width: 10),

        // Details
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.productName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              if (item.variantName != null && item.variantName!.isNotEmpty)
                Text(
                  'Quy cách: ${item.variantName}',
                  style: const TextStyle(fontSize: 11, color: Colors.grey),
                ),
              const SizedBox(height: 4),

              Row(
                children: [
                  Text(
                    isFixed ? _formatVND(item.unitPrice) : 'Báo giá tham khảo: ${_formatVND(item.unitPrice)}',
                    style: TextStyle(
                      color: isFixed ? AppColors.primary : AppColors.accentBrown,
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                  Text(' / ${item.unit}', style: const TextStyle(fontSize: 11, color: Colors.grey)),
                ],
              ),
              const SizedBox(height: 6),

              // Quantity Controls
              Row(
                children: [
                  Container(
                    height: 30,
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey.shade400),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Row(
                      children: [
                        IconButton(
                          iconSize: 14,
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(minWidth: 24, minHeight: 24),
                          icon: const Icon(Icons.remove),
                          onPressed: () {
                            ref.read(cartProvider.notifier).updateQuantity(item.cartItemId, item.quantity - 1.0);
                          },
                        ),
                        Text(
                          item.quantity.toInt().toString(),
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                        ),
                        IconButton(
                          iconSize: 14,
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(minWidth: 24, minHeight: 24),
                          icon: const Icon(Icons.add),
                          onPressed: () {
                            ref.read(cartProvider.notifier).updateQuantity(item.cartItemId, item.quantity + 1.0);
                          },
                        ),
                      ],
                    ),
                  ),
                  const Spacer(),

                  IconButton(
                    icon: const Icon(Icons.delete_outline, color: Colors.red, size: 20),
                    onPressed: () {
                      ref.read(cartProvider.notifier).removeItem(item.cartItemId);
                    },
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _formatVND(int price) {
    if (price <= 0) return 'Liên hệ';
    return '${price.toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (m) => '.')}đ';
  }
}
