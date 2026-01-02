package com.am.marketdata.common.model;

import lombok.Data;
import java.util.List;

@Data
public class NSEIndicesResponse {
    private List<NSEIndex> data;
}
