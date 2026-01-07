import 'dart:ui';
import 'package:flutter/material.dart';

class GlassContainer extends StatelessWidget {
  final Widget child;
  final double blur;
  final double opacity;
  final Color color;
  final BorderRadius? borderRadius;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final BoxBorder? border;
  final List<BoxShadow>? shadows;

  const GlassContainer({
    super.key,
    required this.child,
    this.blur = 10,
    this.opacity = 0.5,
    this.color = Colors.white,
    this.borderRadius,
    this.padding,
    this.margin,
    this.border,
    this.shadows,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      decoration: BoxDecoration(
        color: Colors.transparent, // Color is handled by the glass layer
        borderRadius: borderRadius ?? BorderRadius.circular(16),
        boxShadow: shadows ?? [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: borderRadius ?? BorderRadius.circular(16),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: blur, sigmaY: blur),
          child: Container(
            padding: padding,
            decoration: BoxDecoration(
              color: color.withOpacity(opacity),
              borderRadius: borderRadius ?? BorderRadius.circular(16),
              border: border ?? Border.all(color: Colors.white.withOpacity(0.5)),
            ),
            child: child,
          ),
        ),
      ),
    );
  }
}
