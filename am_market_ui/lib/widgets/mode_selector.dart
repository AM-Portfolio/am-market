import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/mode_provider.dart';
import '../core/app_mode.dart';

/// Widget to display and toggle application mode
class ModeSelector extends StatelessWidget {
  const ModeSelector({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final modeProvider = context.watch<ModeProvider>();
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: theme.colorScheme.primary.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: theme.colorScheme.primary.withOpacity(0.3),
          width: 1,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            modeProvider.currentMode.icon,
            style: const TextStyle(fontSize: 16),
          ),
          const SizedBox(width: 8),
          Text(
            modeProvider.currentMode.displayName,
            style: TextStyle(
              color: theme.colorScheme.primary,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
          const SizedBox(width: 8),
          InkWell(
            onTap: () => modeProvider.toggleMode(),
            borderRadius: BorderRadius.circular(12),
            child: Container(
              padding: const EdgeInsets.all(4),
              child: Icon(
                Icons.swap_horiz,
                size: 16,
                color: theme.colorScheme.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
