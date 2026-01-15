import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart' hide Provider;
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:provider/provider.dart';
import 'package:am_market_common/services/api_service.dart';
import 'package:am_market_ui/features/dashboard/presentation/pages/dashboard_page.dart';
import 'package:am_design_system/am_design_system.dart';

import 'package:get_it/get_it.dart';
import 'package:am_auth_ui/am_auth_ui.dart';
import 'package:am_market_ui/features/market_analysis/services/market_analysis_service.dart';

void main() {
  // Initialize Bundle/DI
  final storage = SecureStorageService();
  GetIt.I.registerSingleton<SecureStorageService>(storage);
  
  // Register Services
  final apiService = ApiService();
  GetIt.I.registerSingleton<ApiService>(apiService);
  GetIt.I.registerSingleton<MarketAnalysisService>(MarketAnalysisService());

  runApp(
    MultiProvider(
      providers: [
        ...AuthProviders.providers,
        BlocProvider<ThemeCubit>(
          create: (context) => ThemeCubit(ThemeRepository()),
        ),
        Provider<ApiService>(
          create: (_) => ApiService(),
        ),
      ],
      child: const ProviderScope(
        child: MarketExampleApp(),
      ),
    ),
  );
}

class MarketExampleApp extends StatelessWidget {
  const MarketExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Market UI Example',
      theme: AppTheme.darkTheme,
      home: AuthWrapper(
        child: BlocBuilder<AuthCubit, AuthState>(
          builder: (context, state) {
            String userId = '';
            if (state is Authenticated) {
              userId = state.user.id;
            }
            return MarketPage(
              userId: userId,
            );
          },
        ),
      ),
      debugShowCheckedModeBanner: false,
    );
  }
}
