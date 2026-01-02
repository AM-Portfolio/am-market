package com.am.marketdata.provider.upstox.model;

import lombok.Data;

@Data
public class AuthCodeResponse {
    private String code;
    private String state;
    private String status;
} 