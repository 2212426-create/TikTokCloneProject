import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../auth/providers/auth_provider.dart';
import '../models/quote_model.dart';
import '../repositories/quote_repository.dart';

/// Structured Card hiển thị Yêu cầu báo giá & Thỏa thuận giá
class QuoteCardWidget extends ConsumerStatefulWidget {
  final QuoteRequestModel quote;

  const QuoteCardWidget({
    super.key,
    required this.quote,
  });

  @override
  ConsumerState<QuoteCardWidget> createState() => _QuoteCardWidgetState();
}

class _QuoteCardWidgetState extends ConsumerState<QuoteCardWidget> {
  final _priceController = TextEditingController();
  final _shippingController = TextEditingController(text: '30000');
  final _notesController = TextEditingController();

  final _addressController = TextEditingController();
  final _phoneController = TextEditingController();

  bool _isSubmitting = false;

  @override
  void dispose() {
    _priceController.dispose();
    _shippingController.dispose();
    _notesController.dispose();
    _addressController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  void _showSellerQuoteModal(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Báo Giá / Thỏa Thuận (Phiên bản ${widget.quote.currentVersion + 1})',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.primary),
              ),
              const SizedBox(height: 12),
              Text('Sản phẩm: ${widget.quote.productName} x ${widget.quote.quantity} ${widget.quote.unit}'),
              const SizedBox(height: 12),

              TextField(
                controller: _priceController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Giá đơn vị sản phẩm (VND) *',
                  hintText: 'VD: 80000',
                ),
              ),
              const SizedBox(height: 10),

              TextField(
                controller: _shippingController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Phí vận chuyển ước tính (VND) *',
                  hintText: 'VD: 35000',
                ),
              ),
              const SizedBox(height: 10),

              TextField(
                controller: _notesController,
                decoration: const InputDecoration(
                  labelText: 'Ghi chú cho người mua',
                  hintText: 'Đóng thùng gỗ / Giao hỏa tốc...',
                ),
              ),
              const SizedBox(height: 16),

              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: _isSubmitting
                      ? null
                      : () async {
                          final itemPrice = int.tryParse(_priceController.text.trim()) ?? 0;
                          final shipFee = int.tryParse(_shippingController.text.trim()) ?? 0;
                          if (itemPrice <= 0) return;

                          setState(() => _isSubmitting = true);
                          try {
                            await ref.read(quoteRepositoryProvider).submitQuoteOffer(
                                  quoteId: widget.quote.id,
                                  itemPrice: itemPrice,
                                  shippingFee: shipFee,
                                  validHours: 48,
                                  notes: _notesController.text.trim(),
                                );
                            if (ctx.mounted) Navigator.pop(ctx);
                          } finally {
                            if (mounted) setState(() => _isSubmitting = false);
                          }
                        },
                  child: const Text('Gửi Báo Giá Mới'),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _showBuyerConfirmModal(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 16,
            right: 16,
            top: 16,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 16,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Xác Nhận Báo Giá & Đặt Hàng',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.primary),
              ),
              const SizedBox(height: 12),

              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(
                  labelText: 'Số điện thoại nhận hàng *',
                  hintText: 'VD: 0987654321',
                ),
              ),
              const SizedBox(height: 10),

              TextField(
                controller: _addressController,
                decoration: const InputDecoration(
                  labelText: 'Địa chỉ nhận hàng chi tiết *',
                  hintText: 'VD: Số 12, Thôn 2, Xã Hiệp An, Đức Trọng',
                ),
              ),
              const SizedBox(height: 16),

              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: _isSubmitting
                      ? null
                      : () async {
                          if (_addressController.text.trim().isEmpty || _phoneController.text.trim().isEmpty) return;

                          setState(() => _isSubmitting = true);
                          try {
                            final orderId = await ref.read(quoteRepositoryProvider).acceptQuoteOffer(
                                  quoteId: widget.quote.id,
                                  deliveryAddress: _addressController.text.trim(),
                                  contactPhone: _phoneController.text.trim(),
                                );
                            if (ctx.mounted) Navigator.pop(ctx);
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(content: Text('Đã tạo đơn hàng từ báo giá thành công!')),
                              );
                              context.push('/orders/$orderId');
                            }
                          } catch (e) {
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(content: Text('Lỗi: $e')),
                              );
                            }
                          } finally {
                            if (mounted) setState(() => _isSubmitting = false);
                          }
                        },
                  child: const Text('Chốt Đơn Hàng Ngay'),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final currentUser = ref.watch(currentUserProvider);
    final isBuyer = currentUser?.uid == widget.quote.buyerId;
    final isSeller = currentUser?.uid == widget.quote.sellerId;
    final version = widget.quote.latestVersion;

    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Status Header
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(Icons.sell_outlined, size: 16, color: AppColors.accentBrown),
                    const SizedBox(width: 4),
                    Text(
                      'Thỏa thuận báo giá (v${widget.quote.currentVersion})',
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                    ),
                  ],
                ),
                _buildStatusBadge(widget.quote.status, widget.quote.isExpired),
              ],
            ),
            const Divider(),

            // Product summary
            Row(
              children: [
                Container(
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: AppColors.primary.withAlpha(20),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: widget.quote.productImage.isNotEmpty
                      ? Image.network(
                          widget.quote.productImage,
                          fit: BoxFit.cover,
                          errorBuilder: (ctx, err, st) => const Icon(Icons.image, color: Colors.grey),
                        )
                      : const Icon(Icons.grass, color: AppColors.primary),
                ),
                const SizedBox(width: 10),

                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.quote.productName,
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                      Text(
                        'Số lượng đặt mua: ${widget.quote.quantity} ${widget.quote.unit}',
                        style: const TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                    ],
                  ),
                ),
              ],
            ),

            // Price Details if quoted
            if (version != null) ...[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.primary.withAlpha(15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Giá đơn vị:'),
                        Text('${_formatVND(version.itemPrice)} / ${widget.quote.unit}'),
                      ],
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Phí giao hàng:'),
                        Text(_formatVND(version.shippingFee)),
                      ],
                    ),
                    const Divider(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Tổng thanh toán:', style: TextStyle(fontWeight: FontWeight.bold)),
                        Text(
                          _formatVND(version.grandTotal),
                          style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.primary, fontSize: 15),
                        ),
                      ],
                    ),
                    if (version.notes.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text('Ghi chú: ${version.notes}', style: const TextStyle(fontSize: 11, color: Colors.grey)),
                    ],
                  ],
                ),
              ),
            ],

            const SizedBox(height: 12),

            // Action Buttons
            Row(
              children: [
                if (isSeller && widget.quote.status != 'accepted') ...[
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => _showSellerQuoteModal(context),
                      child: Text(widget.quote.status == 'pending' ? 'Báo Giá Ngay' : 'Gửi Báo Giá Mới'),
                    ),
                  ),
                ],

                if (isBuyer && widget.quote.status == 'quoted' && !widget.quote.isExpired) ...[
                  Expanded(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(backgroundColor: AppColors.primary),
                      onPressed: () => _showBuyerConfirmModal(context),
                      child: const Text('Xác Nhận & Đặt Hàng', style: TextStyle(color: Colors.white)),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBadge(String status, bool isExpired) {
    if (isExpired && status != 'accepted') {
      return const Chip(
        label: Text('Hết hạn', style: TextStyle(color: Colors.white, fontSize: 10)),
        backgroundColor: Colors.grey,
        materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
      );
    }

    switch (status) {
      case 'pending':
        return const Chip(
          label: Text('Chờ báo giá', style: TextStyle(color: Colors.white, fontSize: 10)),
          backgroundColor: AppColors.warning,
          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
        );
      case 'quoted':
        return const Chip(
          label: Text('Đã báo giá', style: TextStyle(color: Colors.white, fontSize: 10)),
          backgroundColor: AppColors.primary,
          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
        );
      case 'accepted':
        return const Chip(
          label: Text('Đã chốt đơn', style: TextStyle(color: Colors.white, fontSize: 10)),
          backgroundColor: AppColors.success,
          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
        );
      default:
        return Chip(
          label: Text(status, style: const TextStyle(color: Colors.white, fontSize: 10)),
          backgroundColor: Colors.grey,
        );
    }
  }

  String _formatVND(int price) {
    if (price <= 0) return '0đ';
    return '${price.toString().replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (m) => '.')}đ';
  }
}
