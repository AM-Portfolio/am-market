package com.am.common.investment.persistence.document.instrument;

import com.am.common.investment.model.equity.Instrument;
import com.am.common.investment.persistence.document.BaseDocument;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document for storing instrument data
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "instruments")
public class InstrumentDocument extends BaseDocument {
    private Instrument instrument;
}
