/// Model Phiên bản báo giá (Hỗ trợ người bán gửi lại phiên bản mới khi thỏa thuận)
class QuoteVersionModel {
  final int version;
  final int itemPrice;   // Giá sản phẩm đơn vị VND
  final int shippingFee; // Phí giao hàng VND
  final int grandTotal;  // Tổng cộng VND
  final int validUntil;  // Thời hạn hiệu lực (epoch ms)
  final String notes;    // Ghi chú từ người bán
  final int createdAt;

  QuoteVersionModel({
    required this.version,
    required this.itemPrice,
    required this.shippingFee,
    required this.grandTotal,
    required this.validUntil,
    this.notes = '',
    required this.createdAt,
  });

  factory QuoteVersionModel.fromMap(Map<String, dynamic> map) {
    return QuoteVersionModel(
      version: (map['version'] as num?)?.toInt() ?? 1,
      itemPrice: (map['itemPrice'] as num?)?.toInt() ?? 0,
      shippingFee: (map['shippingFee'] as num?)?.toInt() ?? 0,
      grandTotal: (map['grandTotal'] as num?)?.toInt() ?? 0,
      validUntil: (map['validUntil'] as num?)?.toInt() ?? 0,
      notes: map['notes'] as String? ?? '',
      createdAt: (map['createdAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
    );
  }

  Map<String, dynamic> toMap() => {
        'version': version,
        'itemPrice': itemPrice,
        'shippingFee': shippingFee,
        'grandTotal': grandTotal,
        'validUntil': validUntil,
        'notes': notes,
        'createdAt': createdAt,
      };
}

/// Model Yêu Cầu Báo Giá
class QuoteRequestModel {
  final String id;
  final String buyerId;
  final String buyerName;
  final String sellerId;
  final String sellerName;
  final String productId;
  final String productName;
  final String productImage;
  final String? variantName;
  final double quantity;
  final String unit;
  final int currentVersion;
  final String status; // 'pending' | 'quoted' | 'accepted' | 'rejected' | 'expired' | 'cancelled'
  final List<QuoteVersionModel> versions;
  final int createdAt;
  final int updatedAt;

  QuoteRequestModel({
    required this.id,
    required this.buyerId,
    this.buyerName = '',
    required this.sellerId,
    this.sellerName = '',
    required this.productId,
    required this.productName,
    this.productImage = '',
    this.variantName,
    required this.quantity,
    this.unit = 'kg',
    this.currentVersion = 1,
    this.status = 'pending',
    this.versions = const [],
    required this.createdAt,
    required this.updatedAt,
  });

  QuoteVersionModel? get latestVersion {
    if (versions.isEmpty) return null;
    return versions.last;
  }

  bool get isExpired {
    final v = latestVersion;
    if (v == null) return false;
    return DateTime.now().millisecondsSinceEpoch > v.validUntil;
  }

  factory QuoteRequestModel.fromMap(Map<String, dynamic> map, [String? docId]) {
    return QuoteRequestModel(
      id: docId ?? map['id'] as String? ?? '',
      buyerId: map['buyerId'] as String? ?? '',
      buyerName: map['buyerName'] as String? ?? '',
      sellerId: map['sellerId'] as String? ?? '',
      sellerName: map['sellerName'] as String? ?? '',
      productId: map['productId'] as String? ?? '',
      productName: map['productName'] as String? ?? '',
      productImage: map['productImage'] as String? ?? '',
      variantName: map['variantName'] as String?,
      quantity: (map['quantity'] as num?)?.toDouble() ?? 1.0,
      unit: map['unit'] as String? ?? 'kg',
      currentVersion: (map['currentVersion'] as num?)?.toInt() ?? 1,
      status: map['status'] as String? ?? 'pending',
      versions: (map['versions'] as List<dynamic>?)
              ?.map((e) => QuoteVersionModel.fromMap(e as Map<String, dynamic>))
              .toList() ??
          const [],
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
        'productId': productId,
        'productName': productName,
        'productImage': productImage,
        'variantName': variantName,
        'quantity': quantity,
        'unit': unit,
        'currentVersion': currentVersion,
        'status': status,
        'versions': versions.map((v) => v.toMap()).toList(),
        'createdAt': createdAt,
        'updatedAt': updatedAt,
      };
}
