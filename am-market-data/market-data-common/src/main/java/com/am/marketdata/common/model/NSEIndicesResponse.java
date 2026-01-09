package com.am.marketdata.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NSEIndicesResponse {
    private List<NSEIndex> data;
}
