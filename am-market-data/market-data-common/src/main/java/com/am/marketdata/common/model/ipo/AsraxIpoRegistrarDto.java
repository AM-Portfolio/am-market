package com.am.marketdata.common.model.ipo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified ASRAX domain DTO representing the registrar contact information and portal details for an IPO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsraxIpoRegistrarDto {

    /**
     * Full company name of the registrar (e.g., "MUFG INTIME INDIA PRIVATE LIMITED").
     */
    private String name;

    /**
     * Customer support email address of the registrar.
     */
    private String email;

    /**
     * Contact officer person name.
     */
    private String contactName;

    /**
     * Contact phone number of registrar support.
     */
    private String contactNumber;

    /**
     * Official website URL of the registrar for checking allotment status.
     */
    private String website;

    /**
     * Short registrar identifier key (e.g., "MUFG", "LINKINTIME", "KFINTECH", "SKYLINE").
     */
    private String registrarKey;
}
