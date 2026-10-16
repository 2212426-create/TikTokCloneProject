/// Model dòng sản phẩm trong Giỏ hàng
class CartItemModel {
  final String cartItemId;
  final String productId;
  final String sellerId;
  final String sellerName;
  final String productName;
  final String productImage;
  final String? variantId;
  final String? variantName;
  final String unit;
  final String saleType; // 'fixed_price' | 'quote_required'
  final int unitPrice;   // VND integer
  final double quantity;
  final bool isSelected;
  final bool isAvailable;

  CartItemModel({
    required this.cartItemId,
    required this.productId,
    required this.sellerId,
    required this.sellerName,
    required this.productName,
    this.productImage = '',
    this.variantId,
    this.variantName,
    this.unit = 'kg',
    required this.saleType,
    required this.unitPrice,
    required this.quantity,
    this.isSelected = true,
    this.isAvailable = true,
  });

  int get totalPrice => (saleType == 'fixed_price') ? (unitPrice * quantity).round() : 0;

  CartItemModel copyWith({
    String? cartItemId,
    String? productId,
    String? sellerId,
    String? sellerName,
    String? productName,
    String? productImage,
    String? variantId,
    String? variantName,
    String? unit,
    String? saleType,
    int? unitPrice,
    double? quantity,
    bool? isSelected,
    bool? isAvailable,
  }) {
    return CartItemModel(
      cartItemId: cartItemId ?? this.cartItemId,
      productId: productId ?? this.productId,
      sellerId: sellerId ?? this.sellerId,
      sellerName: sellerName ?? this.sellerName,
      productName: productName ?? this.productName,
      productImage: productImage ?? this.productImage,
      variantId: variantId ?? this.variantId,
      variantName: variantName ?? this.variantName,
      unit: unit ?? this.unit,
      saleType: saleType ?? this.saleType,
      unitPrice: unitPrice ?? this.unitPrice,
      quantity: quantity ?? this.quantity,
      isSelected: isSelected ?? this.isSelected,
      isAvailable: isAvailable ?? this.isAvailable,
    );
  }

  factory CartItemModel.fromMap(Map<String, dynamic> map) {
    return CartItemModel(
      cartItemId: map['cartItemId'] as String? ?? '',
      productId: map['productId'] as String? ?? '',
      sellerId: map['sellerId'] as String? ?? '',
      sellerName: map['sellerName'] as String? ?? '',
      productName: map['productName'] as String? ?? '',
      productImage: map['productImage'] as String? ?? '',
      variantId: map['variantId'] as String?,
      variantName: map['variantName'] as String?,
      unit: map['unit'] as String? ?? 'kg',
      saleType: map['saleType'] as String? ?? 'fixed_price',
      unitPrice: (map['unitPrice'] as num?)?.toInt() ?? 0,
      quantity: (map['quantity'] as num?)?.toDouble() ?? 1.0,
      isSelected: map['isSelected'] as bool? ?? true,
      isAvailable: map['isAvailable'] as bool? ?? true,
    );
  }

  Map<String, dynamic> toMap() => {
        'cartItemId': cartItemId,
        'productId': productId,
        'sellerId': sellerId,
        'sellerName': sellerName,
        'productName': productName,
        'productImage': productImage,
        'variantId': variantId,
        'variantName': variantName,
        'unit': unit,
        'saleType': saleType,
        'unitPrice': unitPrice,
        'quantity': quantity,
        'isSelected': isSelected,
        'isAvailable': isAvailable,
      };
}

/// Nhóm giỏ hàng theo từng Người bán (Seller)
class CartGroupModel {
  final String sellerId;
  final String sellerName;
  final List<CartItemModel> items;

  CartGroupModel({
    required this.sellerId,
    required this.sellerName,
    required this.items,
  });

  bool get isAllSelected => items.every((i) => i.isSelected);

  int get selectedFixedPriceTotal {
    return items
        .where((i) => i.isSelected && i.saleType == 'fixed_price' && i.isAvailable)
        .fold(0, (sum, item) => sum + item.totalPrice);
  }
}
