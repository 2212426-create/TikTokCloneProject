import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/theme/app_colors.dart';
import '../../auth/providers/auth_provider.dart';
import '../../profile/providers/profile_provider.dart';
import '../../profile/repositories/profile_repository.dart';
import '../models/store_model.dart';
import '../repositories/store_repository.dart';

/// Màn hình Đăng ký Gian hàng Bán hàng Đa bước (Có Lưu nháp & Tải giấy tờ)
class StoreRegistrationScreen extends ConsumerStatefulWidget {
  const StoreRegistrationScreen({super.key});

  @override
  ConsumerState<StoreRegistrationScreen> createState() => _StoreRegistrationScreenState();
}

class _StoreRegistrationScreenState extends ConsumerState<StoreRegistrationScreen> {
  int _currentStep = 0;
  final _formKey = GlobalKey<FormState>();

  // Step 1: Type
  String _storeType = 'individual'; // 'individual' | 'business'
  String _businessSubtype = 'enterprise'; // 'enterprise' | 'agent' | 'coop'

  // Step 2: Details
  final _storeNameController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _contactNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _regionController = TextEditingController(text: 'Lâm Đồng');
  final _addressController = TextEditingController();
  final _pickupLocationController = TextEditingController();

  String _avatarUrl = '';
  String _coverUrl = '';

  final List<String> _selectedCategories = ['Nông sản'];
  final List<String> _selectedFulfillment = ['pickup', 'agreed_delivery'];

  // Step 3: Verification
  final _taxCodeController = TextEditingController();
  final _representativeController = TextEditingController();
  final List<String> _verificationDocs = [];

  bool _isUploadingMedia = false;
  bool _isSubmitting = false;
  String? _existingStoreId;

  final ImagePicker _picker = ImagePicker();

  final List<String> _allCategories = [
    'Nông sản',
    'Vật tư nông nghiệp',
    'Phân bón & Thuốc BVTV',
    'Cây giống & Hạt giống',
    'Thiết bị & Máy nông nghiệp',
  ];

  @override
  void initState() {
    super.initState();
    _loadExistingDraft();
  }

  Future<void> _loadExistingDraft() async {
    final currentUser = ref.read(currentUserProvider);
    if (currentUser == null) return;

    final profile = ref.read(currentUserProfileProvider).valueOrNull;
    if (profile != null) {
      _contactNameController.text = profile.fullname;
    }

    final store = await ref.read(storeRepositoryProvider).getStoreByOwner(currentUser.uid);
    if (store != null && mounted) {
      _existingStoreId = store.storeId;
      _storeType = store.storeType;
      if (store.businessSubtype != null) _businessSubtype = store.businessSubtype!;
      _storeNameController.text = store.storeName;
      _descriptionController.text = store.description;
      _contactNameController.text = store.contactName;
      _phoneController.text = store.phone;
      _regionController.text = store.region;
      _addressController.text = store.address;
      if (store.pickupLocation != null) _pickupLocationController.text = store.pickupLocation!;
      _avatarUrl = store.avatarUrl;
      _coverUrl = store.coverUrl;
      if (store.taxCode != null) _taxCodeController.text = store.taxCode!;
      if (store.representative != null) _representativeController.text = store.representative!;
      _verificationDocs.addAll(store.verificationDocs);
      setState(() {});
    }
  }

  @override
  void dispose() {
    _storeNameController.dispose();
    _descriptionController.dispose();
    _contactNameController.dispose();
    _phoneController.dispose();
    _regionController.dispose();
    _addressController.dispose();
    _pickupLocationController.dispose();
    _taxCodeController.dispose();
    _representativeController.dispose();
    super.dispose();
  }

  Future<void> _pickAndUploadImage(String type) async {
    final XFile? file = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 85);
    if (file == null) return;

    final currentUser = ref.read(currentUserProvider);
    if (currentUser == null) return;

    setState(() => _isUploadingMedia = true);

    try {
      final downloadUrl = await ref.read(profileRepositoryProvider).updateAvatar(currentUser.uid, file);
      setState(() {
        if (type == 'avatar') _avatarUrl = downloadUrl;
        if (type == 'cover') _coverUrl = downloadUrl;
        if (type == 'doc') _verificationDocs.add(downloadUrl);
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Tải ảnh lên thành công!')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi tải ảnh: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isUploadingMedia = false);
    }
  }

  StoreModel _buildStoreFromForm(String status) {
    final currentUser = ref.read(currentUserProvider)!;
    final profile = ref.read(currentUserProfileProvider).valueOrNull;

    return StoreModel(
      storeId: _existingStoreId ?? '',
      ownerId: currentUser.uid,
      ownerName: profile?.username ?? currentUser.email?.split('@')[0] ?? '',
      storeType: _storeType,
      businessSubtype: _storeType == 'business' ? _businessSubtype : null,
      storeName: _storeNameController.text.trim(),
      avatarUrl: _avatarUrl,
      coverUrl: _coverUrl,
      description: _descriptionController.text.trim(),
      contactName: _contactNameController.text.trim(),
      phone: _phoneController.text.trim(),
      address: _addressController.text.trim(),
      region: _regionController.text.trim(),
      pickupLocation: _pickupLocationController.text.trim().isNotEmpty ? _pickupLocationController.text.trim() : null,
      targetCategories: _selectedCategories,
      fulfillmentMethods: _selectedFulfillment,
      verificationDocs: _verificationDocs,
      taxCode: _taxCodeController.text.trim().isNotEmpty ? _taxCodeController.text.trim() : null,
      representative: _representativeController.text.trim().isNotEmpty ? _representativeController.text.trim() : null,
      status: status,
      operatingStatus: 'active',
      createdAt: DateTime.now().millisecondsSinceEpoch,
      updatedAt: DateTime.now().millisecondsSinceEpoch,
    );
  }

  Future<void> _saveDraft() async {
    final currentUser = ref.read(currentUserProvider);
    if (currentUser == null) return;

    setState(() => _isSubmitting = true);
    try {
      final store = _buildStoreFromForm('draft');
      final sId = await ref.read(storeRepositoryProvider).saveStoreDraft(store);
      _existingStoreId = sId;

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã lưu bản nháp đăng ký gian hàng!')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _submitRegistration() async {
    if (!_formKey.currentState!.validate()) return;
    final currentUser = ref.read(currentUserProvider);
    if (currentUser == null) return;

    setState(() => _isSubmitting = true);
    try {
      final store = _buildStoreFromForm('pending');
      await ref.read(storeRepositoryProvider).submitStoreRegistration(store);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Gửi hồ sơ đăng ký thành công! Đang chờ BQT xét duyệt.')),
        );
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi gửi hồ sơ: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Đăng Ký Bán Hàng'),
        actions: [
          TextButton(
            onPressed: _isSubmitting ? null : _saveDraft,
            child: const Text('Lưu nháp', style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: Stepper(
          currentStep: _currentStep,
          onStepContinue: () {
            if (_currentStep < 3) {
              setState(() => _currentStep += 1);
            } else {
              _submitRegistration();
            }
          },
          onStepCancel: () {
            if (_currentStep > 0) {
              setState(() => _currentStep -= 1);
            }
          },
          steps: [
            // Step 1:Hình thức
            Step(
              title: const Text('Hình thức bán hàng'),
              isActive: _currentStep >= 0,
              content: Column(
                children: [
                  RadioListTile<String>(
                    title: const Text('Cá nhân / Nhà vườn'),
                    subtitle: const Text('Bán nông sản, trái cây, nông sản thu hoạch từ vườn nhà'),
                    value: 'individual',
                    groupValue: _storeType,
                    onChanged: (val) => setState(() => _storeType = val!),
                  ),
                  RadioListTile<String>(
                    title: const Text('Đơn vị kinh doanh'),
                    subtitle: const Text('Doanh nghiệp, Đại lý vật tư, Hợp tác xã (HTX)'),
                    value: 'business',
                    groupValue: _storeType,
                    onChanged: (val) => setState(() => _storeType = val!),
                  ),

                  if (_storeType == 'business') ...[
                    const SizedBox(height: 8),
                    DropdownButtonFormField<String>(
                      value: _businessSubtype,
                      decoration: const InputDecoration(labelText: 'Loại hình kinh doanh *'),
                      items: const [
                        DropdownMenuItem(value: 'enterprise', child: Text('Doanh nghiệp nông nghiệp')),
                        DropdownMenuItem(value: 'agent', child: Text('Đại lý vật tư / Phân bón')),
                        DropdownMenuItem(value: 'coop', child: Text('Hợp tác xã (HTX)')),
                      ],
                      onChanged: (val) {
                        if (val != null) setState(() => _businessSubtype = val);
                      },
                    ),
                  ],
                ],
              ),
            ),

            // Step 2: Thông tin gian hàng
            Step(
              title: const Text('Thông tin gian hàng'),
              isActive: _currentStep >= 1,
              content: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TextFormField(
                    controller: _storeNameController,
                    decoration: const InputDecoration(
                      labelText: 'Tên gian hàng *',
                      hintText: 'VD: Nông sản Sạch Đức Trọng / Đại lý VTNN Phú Nông',
                    ),
                    validator: (v) => v == null || v.trim().isEmpty ? 'Nhập tên gian hàng' : null,
                  ),
                  const SizedBox(height: 12),

                  TextFormField(
                    controller: _contactNameController,
                    decoration: const InputDecoration(
                      labelText: 'Người đại diện / Người liên hệ *',
                    ),
                    validator: (v) => v == null || v.trim().isEmpty ? 'Nhập tên người liên hệ' : null,
                  ),
                  const SizedBox(height: 12),

                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                      labelText: 'Số điện thoại liên hệ *',
                    ),
                    validator: (v) => v == null || v.trim().isEmpty ? 'Nhập SĐT gian hàng' : null,
                  ),
                  const SizedBox(height: 12),

                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _regionController,
                          decoration: const InputDecoration(labelText: 'Tỉnh / Khu vực *'),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Nhập khu vực' : null,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextFormField(
                          controller: _addressController,
                          decoration: const InputDecoration(labelText: 'Địa chỉ chi tiết *'),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Nhập địa chỉ' : null,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // Avatar / Cover photo picking via ImagePicker
                  Row(
                    children: [
                      ElevatedButton.icon(
                        icon: const Icon(Icons.photo_camera),
                        label: const Text('Chọn Avatar Gian hàng'),
                        onPressed: _isUploadingMedia ? null : () => _pickAndUploadImage('avatar'),
                      ),
                    ],
                  ),
                  if (_avatarUrl.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    Text('Avatar: $_avatarUrl', style: const TextStyle(fontSize: 10, color: Colors.green)),
                  ],
                  const SizedBox(height: 12),

                  TextFormField(
                    controller: _descriptionController,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: 'Giới thiệu gian hàng',
                      hintText: 'Kinh nghiệm, sản phẩm chủ lực, cam kết chất lượng...',
                    ),
                  ),
                ],
              ),
            ),

            // Step 3: Giấy tờ xác minh
            Step(
              title: const Text('Hồ sơ xác minh'),
              isActive: _currentStep >= 2,
              content: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_storeType == 'business') ...[
                    TextFormField(
                      controller: _taxCodeController,
                      decoration: const InputDecoration(
                        labelText: 'Mã số thuế / Mã ĐKKD *',
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _representativeController,
                      decoration: const InputDecoration(
                        labelText: 'Người đại diện pháp luật *',
                      ),
                    ),
                    const SizedBox(height: 12),
                  ],

                  const Text('Giấy tờ xác minh (CCCD / ĐKKD / Giấy chứng nhận HTX):'),
                  const SizedBox(height: 6),
                  ElevatedButton.icon(
                    icon: const Icon(Icons.upload_file),
                    label: const Text('Tải ảnh Giấy tờ xác minh'),
                    onPressed: _isUploadingMedia ? null : () => _pickAndUploadImage('doc'),
                  ),
                  const SizedBox(height: 6),
                  Text('Đã tải ${_verificationDocs.length} tệp tài liệu xác minh (Được bảo mật riêng tư).', style: const TextStyle(fontSize: 12, color: Colors.grey)),
                ],
              ),
            ),

            // Step 4: Kiểm tra & Gửi
            Step(
              title: const Text('Kiểm tra & Gửi'),
              isActive: _currentStep >= 3,
              content: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Tóm tắt hồ sơ đăng ký:', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.primary)),
                          const SizedBox(height: 6),
                          Text('Tên gian hàng: ${_storeNameController.text}'),
                          Text('Loại hình: ${_storeType == "individual" ? "Cá nhân / Nhà vườn" : "Đơn vị kinh doanh"}'),
                          Text('Khu vực: ${_regionController.text}'),
                          Text('SĐT: ${_phoneController.text}'),
                          Text('Tài liệu xác minh: ${_verificationDocs.length} tệp'),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    '🔒 Lưu ý: Giấy tờ xác minh được lưu bảo mật và chỉ dùng cho BQT kiểm duyệt, không công khai lên gian hàng.',
                    style: TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
