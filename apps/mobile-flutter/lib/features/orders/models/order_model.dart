/// Snapshot của từng mặt hàng trong đơn hàng tại thời điểm chốt
class OrderItemSnapshot {
  final String productId;
  final String productName;
  final String productImage;
  final String? variantId;
  final String? variantName;
  final String unit;
  final int price;     // VND
  final double quantity;
  final int itemTotal; // VND

  OrderItemSnapshot({
    required this.productId,
    required this.productName,
    this.productImage = '',
    this.variantId,
    this.variantName,
    this.unit = 'kg',
    required this.price,
    required this.quantity,
    required this.itemTotal,
  });

  factory OrderItemSnapshot.fromMap(Map<String, dynamic> map) {
    return OrderItemSnapshot(
      productId: map['productId'] as String? ?? '',
      productName: map['productName'] as String? ?? '',
      productImage: map['productImage'] as String? ?? '',
      variantId: map['variantId'] as String?,
      variantName: map['variantName'] as String?,
      unit: map['unit'] as String? ?? 'kg',
      price: (map['price'] as num?)?.toInt() ?? 0,
      quantity: (map['quantity'] as num?)?.toDouble() ?? 1.0,
      itemTotal: (map['itemTotal'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toMap() => {
        'productId': productId,
        'productName': productName,
        'productImage': productImage,
        'variantId': variantId,
        'variantName': variantName,
        'unit': unit,
        'price': price,
        'quantity': quantity,
        'itemTotal': itemTotal,
      };
}

/// Model Đơn hàng nông sản
class OrderModel {
  final String id;
  final String buyerId;
  final String buyerName;
  final String sellerId;
  final String sellerName;
  final List<OrderItemSnapshot> items;
  final int itemTotal;
  final int shippingFee;
  final bool isShippingFeeKnown;
  final int grandTotal;
  final String fulfillmentType; // 'pickup' | 'agreed_delivery'
  final String paymentMethod;    // 'cod' | 'pickup_pay'
  final String contactPhone;
  final String deliveryAddress;
  final String status;          // 'pending_seller_confirmation' | 'pending_buyer_confirmation' | 'confirmed' | 'shipping' | 'completed' | 'cancelled'
  final String? quoteId;
  final int createdAt;
  final int updatedAt;

  OrderModel({
    required this.id,
    required this.buyerId,
    this.buyerName = '',
    required this.sellerId,
    this.sellerName = '',
    required this.items,
    required this.itemTotal,
    this.shippingFee = 0,
    this.isShippingFeeKnown = true,
    required this.grandTotal,
    this.fulfillmentType = 'agreed_delivery',
    this.paymentMethod = 'cod',
    required this.contactPhone,
    required this.deliveryAddress,
    this.status = 'pending_seller_confirmation',
    this.quoteId,
    required this.createdAt,
    required this.updatedAt,
  });

  factory OrderModel.fromMap(Map<String, dynamic> map, [String? docId]) {
    return OrderModel(
      id: docId ?? map['id'] as String? ?? '',
      buyerId: map['buyerId'] as String? ?? '',
      buyerName: map['buyerName'] as String? ?? '',
      sellerId: map['sellerId'] as String? ?? '',
      sellerName: map['sellerName'] as String? ?? '',
      items: (map['items'] as List<dynamic>?)
              ?.map((e) => OrderItemSnapshot.fromMap(e as Map<String, dynamic>))
              .toList() ??
          const [],
      itemTotal: (map['itemTotal'] as num?)?.toInt() ?? 0,
      shippingFee: (map['shippingFee'] as num?)?.toInt() ?? 0,
      isShippingFeeKnown: map['isShippingFeeKnown'] as bool? ?? true,
      grandTotal: (map['grandTotal'] as num?)?.toInt() ?? 0,
      fulfillmentType: map['fulfillmentType'] as String? ?? 'agreed_delivery',
      paymentMethod: map['paymentMethod'] as String? ?? 'cod',
      contactPhone: map['contactPhone'] as String? ?? '',
      deliveryAddress: map['deliveryAddress'] as String? ?? '',
      status: map['status'] as String? ?? 'pending_seller_confirmation',
      quoteId: map['quoteId'] as String?,
      createdAt: (map['createdAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      updatedAt: (map['updatedAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'buyerId': buyerId,
        'buyerName': buyerName,
        'sellerId': sellerId,
        'sellerName': sellerName,
        'items': items.map((i) => i.toMap()).toList(),
        'itemTotal': itemTotal,
        'shippingFee': shippingFee,
        'isShippingFeeKnown': isShippingFeeKnown,
        'grandTotal': grandTotal,
        'fulfillmentType': fulfillmentType,
        'paymentMethod': paymentMethod,
        'contactPhone': contactPhone,
        'deliveryAddress': deliveryAddress,
        'status': status,
        'quoteId': quoteId,
        'createdAt': createdAt,
        'updatedAt': updatedAt,
      };
}
