package com.am.marketdata.common.model.events;

import com.am.common.investment.model.board.DirectorType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoardOfDirector {
    @JsonProperty("DIRNAME")
    private String directorName;

    @JsonProperty("Reported_DSG")
    private String designation;

    public DirectorType getDirectorType() {
        return DirectorType.fromDesignation(designation
                .replace(" & ", "_")
                .replace(".", "")
                .replace(" ", "_")
                .toUpperCase());
    }
}
