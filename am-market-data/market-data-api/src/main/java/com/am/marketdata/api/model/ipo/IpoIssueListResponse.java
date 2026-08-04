package com.am.marketdata.api.model.ipo;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "IpoIssueListResponse", description = "Pageless list of IPO issues")
public class IpoIssueListResponse {

    @ArraySchema(schema = @Schema(implementation = IpoIssueView.class))
    @Builder.Default
    private List<IpoIssueView> data = new ArrayList<>();

    @Schema(description = "Number of issues in data", example = "3")
    private int count;
}
