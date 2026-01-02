package com.am.common.investment.service.mapper;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.common.investment.persistence.document.StockIndicesMarketDataDocument;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.mapstruct.InjectionStrategy;

import java.util.List;

/**
 * Mapper for converting between StockIndicesMarketData model and StockIndicesMarketDataDocument
 */
@Mapper(
    componentModel = "spring",
    uses = {},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface StockIndicesMarketDataMapper {

    StockIndicesMarketDataMapper INSTANCE = Mappers.getMapper(StockIndicesMarketDataMapper.class);

    /**
     * Convert from document to model
     */
    StockIndicesMarketData toModel(StockIndicesMarketDataDocument document);

    /**
     * Convert from model to document
     */
    StockIndicesMarketDataDocument toDocument(StockIndicesMarketData model);

    /**
     * Convert a list of documents to models
     */
    List<StockIndicesMarketData> toModelList(List<StockIndicesMarketDataDocument> documents);

    /**
     * Convert a list of models to documents
     */
    List<StockIndicesMarketDataDocument> toDocumentList(List<StockIndicesMarketData> models);
}
