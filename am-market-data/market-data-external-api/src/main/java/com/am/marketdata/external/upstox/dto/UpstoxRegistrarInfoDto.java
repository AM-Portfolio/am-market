package com.am.marketdata.external.upstox.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Raw Upstox JSON DTO mapping registrar contact and web portal info.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxRegistrarInfoDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("contact_name")
    private String contactName;

    @JsonProperty("contact_number")
    private String contactNumber;

    @JsonProperty("website")
    private String website;

    @JsonProperty("registrar")
    private String registrar;
}
