/// Model quy cách / biến thể sản phẩm nông nghiệp
class ProductVariantModel {
  final String variantId;
  final String name;
  final int price;
  final int stock;
  final double? weightKg;

  ProductVariantModel({
    required this.variantId,
    required this.name,
    required this.price,
    required this.stock,
    this.weightKg,
  });

  factory ProductVariantModel.fromMap(Map<String, dynamic> map) {
    return ProductVariantModel(
      variantId: map['variantId'] as String? ?? '',
      name: map['name'] as String? ?? '',
      price: (map['price'] as num?)?.toInt() ?? 0,
      stock: (map['stock'] as num?)?.toInt() ?? 0,
      weightKg: (map['weightKg'] as num?)?.toDouble(),
    );
  }

  Map<String, dynamic> toMap() => {
        'variantId': variantId,
        'name': name,
        'price': price,
        'stock': stock,
        'weightKg': weightKg,
      };
}

/// Model sản phẩm nông nghiệp trên Chợ & Gắn video (Lưu cả storeId và sellerId)
class ProductModel {
  final String id;
  final String? storeId; // ID gian hàng
  final String sellerId; // ID tài khoản người bán
  final String sellerName;
  final String sellerPhone;
  final String name;
  final String description;
  final List<String> images;
  final String category;
  final List<ProductVariantModel> variants;
  final String unit;
  final bool isDecimalAllowed;
  final String saleType; // 'fixed_price' | 'quote_required'
  final int listedPrice;
  final int referencePrice;
  final int stock;
  final double minOrderQuantity;
  final double quantityStep;
  final String region;
  final List<String> fulfillmentTypes;
  final String deliveryScope;
  final String status;          // 'active' | 'hidden'
  final String moderationStatus;// 'pending' | 'approved' | 'rejected'
  final String? rejectionReason;
  final int createdAt;

  ProductModel({
    required this.id,
    this.storeId,
    required this.sellerId,
    this.sellerName = '',
    this.sellerPhone = '',
    required this.name,
    required this.description,
    this.images = const [],
    required this.category,
    this.variants = const [],
    this.unit = 'kg',
    this.isDecimalAllowed = false,
    this.saleType = 'fixed_price',
    this.listedPrice = 0,
    this.referencePrice = 0,
    this.stock = 0,
    this.minOrderQuantity = 1.0,
    this.quantityStep = 1.0,
    required this.region,
    this.fulfillmentTypes = const ['pickup', 'agreed_delivery'],
    this.deliveryScope = 'Toàn quốc',
    this.status = 'active',
    this.moderationStatus = 'approved',
    this.rejectionReason,
    required this.createdAt,
  });

  factory ProductModel.fromMap(Map<String, dynamic> map, [String? docId]) {
    final sellerIdVal = map['sellerId'] as String? ?? '';
    return ProductModel(
      id: docId ?? map['id'] as String? ?? '',
      storeId: map['storeId'] as String? ?? sellerIdVal,
      sellerId: sellerIdVal,
      sellerName: map['sellerName'] as String? ?? '',
      sellerPhone: map['sellerPhone'] as String? ?? '',
      name: map['name'] as String? ?? '',
      description: map['description'] as String? ?? '',
      images: (map['images'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
      category: map['category'] as String? ?? 'Nông sản',
      variants: (map['variants'] as List<dynamic>?)
              ?.map((e) => ProductVariantModel.fromMap(e as Map<String, dynamic>))
              .toList() ??
          const [],
      unit: map['unit'] as String? ?? 'kg',
      isDecimalAllowed: map['isDecimalAllowed'] as bool? ?? false,
      saleType: map['saleType'] as String? ?? 'fixed_price',
      listedPrice: (map['listedPrice'] as num?)?.toInt() ?? 0,
      referencePrice: (map['referencePrice'] as num?)?.toInt() ?? 0,
      stock: (map['stock'] as num?)?.toInt() ?? 0,
      minOrderQuantity: (map['minOrderQuantity'] as num?)?.toDouble() ?? 1.0,
      quantityStep: (map['quantityStep'] as num?)?.toDouble() ?? 1.0,
      region: map['region'] as String? ?? '',
      fulfillmentTypes: (map['fulfillmentTypes'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const ['pickup', 'agreed_delivery'],
      deliveryScope: map['deliveryScope'] as String? ?? 'Toàn quốc',
      status: map['status'] as String? ?? 'active',
      moderationStatus: map['moderationStatus'] as String? ?? 'approved',
      rejectionReason: map['rejectionReason'] as String?,
      createdAt: (map['createdAt'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
    );
  }

  Map<String, dynamic> toMap() => {
        'id': id,
        'storeId': storeId ?? sellerId,
        'sellerId': sellerId,
        'sellerName': sellerName,
        'sellerPhone': sellerPhone,
        'name': name,
        'description': description,
        'images': images,
        'category': category,
        'variants': variants.map((v) => v.toMap()).toList(),
        'unit': unit,
        'isDecimalAllowed': isDecimalAllowed,
        'saleType': saleType,
        'listedPrice': listedPrice,
        'referencePrice': referencePrice,
        'stock': stock,
        'minOrderQuantity': minOrderQuantity,
        'quantityStep': quantityStep,
        'region': region,
        'fulfillmentTypes': fulfillmentTypes,
        'deliveryScope': deliveryScope,
        'status': status,
        'moderationStatus': moderationStatus,
        'rejectionReason': rejectionReason,
        'createdAt': createdAt,
      };
}
