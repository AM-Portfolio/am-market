package com.am.marketdata.upstock.model;

import lombok.Data;

@Data
public class AuthCodeResponse {
    private String code;
    private String state;
    private String status;
} 