import 'package:flutter/material.dart';
import 'package:am_market_ui/core/providers/view_mode_provider.dart';
import 'package:provider/provider.dart';

/// Mode toggle widget for switching between User and Developer modes
class ModeToggleWidget extends StatelessWidget {
  const ModeToggleWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ViewModeProvider>(
      builder: (context, viewModeProvider, _) {
        if (!viewModeProvider.isInitialized) {
          return const SizedBox.shrink();
        }

        return Container(
          margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          padding: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            color: const Color(0xFF1A1D26),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: Colors.white.withOpacity(0.1),
              width: 1,
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: _ModeButton(
                  label: 'User',
                  icon: Icons.person_rounded,
                  isSelected: viewModeProvider.isUserMode,
                  onTap: () => viewModeProvider.setMode(ViewMode.user),
                ),
              ),
              const SizedBox(width: 4),
              Expanded(
                child: _ModeButton(
                  label: 'Developer',
                  icon: Icons.developer_mode_rounded,
                  isSelected: viewModeProvider.isDeveloperMode,
                  onTap: () => viewModeProvider.setMode(ViewMode.developer),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _ModeButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;

  const _ModeButton({
    required this.label,
    required this.icon,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      child: Material(
        color: isSelected 
            ? const Color(0xFF00D1FF).withOpacity(0.15)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(8),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon,
                  size: 16,
                  color: isSelected 
                      ? const Color(0xFF00D1FF)
                      : Colors.white.withOpacity(0.5),
                ),
                const SizedBox(width: 6),
                Text(
                  label,
                  style: TextStyle(
                    color: isSelected 
                        ? const Color(0xFF00D1FF)
                        : Colors.white.withOpacity(0.5),
                    fontSize: 12,
                    fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
