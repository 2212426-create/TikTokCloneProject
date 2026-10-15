import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/theme/app_colors.dart';

/// Navigation Shell cho Chế độ Bán hàng (Seller Workspace)
/// Các tab: Tổng quan, Sản phẩm, Đơn bán, Tin nhắn, Gian hàng
class SellerBottomNavShell extends ConsumerWidget {
  final StatefulNavigationShell navigationShell;

  const SellerBottomNavShell({super.key, required this.navigationShell});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final currentIndex = navigationShell.currentIndex;

    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: Container(
        color: isDark ? AppColors.darkBg : AppColors.lightBg,
        child: SafeArea(
          top: false,
          child: Container(
            height: 60,
            decoration: BoxDecoration(
              border: Border(
                top: BorderSide(
                  color: isDark ? AppColors.darkBorder : AppColors.lightBorder,
                  width: 0.5,
                ),
              ),
            ),
            child: Row(
              children: [
                Expanded(
                  child: _SellerNavItem(
                    icon: Icons.dashboard_outlined,
                    activeIcon: Icons.dashboard,
                    label: 'Tổng quan',
                    isActive: currentIndex == 0,
                    onTap: () => _onTap(context, 0),
                  ),
                ),
                Expanded(
                  child: _SellerNavItem(
                    icon: Icons.inventory_2_outlined,
                    activeIcon: Icons.inventory_2,
                    label: 'Sản phẩm',
                    isActive: currentIndex == 1,
                    onTap: () => _onTap(context, 1),
                  ),
                ),
                Expanded(
                  child: _SellerNavItem(
                    icon: Icons.receipt_long_outlined,
                    activeIcon: Icons.receipt_long,
                    label: 'Đơn bán',
                    isActive: currentIndex == 2,
                    onTap: () => _onTap(context, 2),
                  ),
                ),
                Expanded(
                  child: _SellerNavItem(
                    icon: Icons.chat_bubble_outline,
                    activeIcon: Icons.chat_bubble,
                    label: 'Tin nhắn',
                    isActive: currentIndex == 3,
                    onTap: () => _onTap(context, 3),
                  ),
                ),
                Expanded(
                  child: _SellerNavItem(
                    icon: Icons.store_outlined,
                    activeIcon: Icons.store,
                    label: 'Gian hàng',
                    isActive: currentIndex == 4,
                    onTap: () => _onTap(context, 4),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _onTap(BuildContext context, int index) {
    HapticFeedback.selectionClick();
    navigationShell.goBranch(
      index,
      initialLocation: index == navigationShell.currentIndex,
    );
  }
}

class _SellerNavItem extends StatelessWidget {
  final IconData icon;
  final IconData activeIcon;
  final String label;
  final bool isActive;
  final VoidCallback onTap;

  const _SellerNavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
    required this.isActive,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final activeColor = AppColors.primary;
    final inactiveColor = isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary;

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: SizedBox(
        height: 60,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isActive ? activeIcon : icon,
              color: isActive ? activeColor : inactiveColor,
              size: 24,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                color: isActive ? activeColor : inactiveColor,
                fontSize: 10,
                fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
