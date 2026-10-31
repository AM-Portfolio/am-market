package com.am.marketdata.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Persisted alias mapping for symbols and ISINs.
 * Used to translate external ticker aliases (e.g. VODAFONEIDEA -> IDEA)
 * and corporate-action stale ISINs (e.g. INE237A01028 -> INE237A01036)
 * without duplicating instrument rows or hardcoding identifiers in application code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "symbol_aliases")
public class SymbolAlias {

    @Id
    private String id;

    private String target;

    private String type; // "SYMBOL" or "ISIN"
}
