package com.am.marketdata.common.resolver;

import java.util.List;
import java.util.Map;

public interface SymbolResolver {
    Map<String, String> resolveIndices(List<String> symbols);
}
