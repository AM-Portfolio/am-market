package com.am.marketdata.processor.service.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.am.common.investment.model.board.BoardOfDirectors;
import com.am.common.investment.model.board.Director;
import com.am.common.investment.model.equity.financial.BaseModel;
import com.am.marketdata.common.model.events.BoardOfDirector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper for converting between BoardOfDirectors and BoardOfDirector
 */
@Component
@Slf4j
public class StockBoardOfDirectorsMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public List<BoardOfDirector> parseDirectors(String jsonData) {
        return objectMapper.readValue(jsonData, 
            TypeFactory.defaultInstance().constructCollectionType(List.class, BoardOfDirector.class));
    }

    private final BaseModelMapper baseModelMapper = new BaseModelMapper();
    
    /**
     * Create BoardOfDirectors from symbol and list of directors
     * 
     * @param symbol Stock symbol
     * @param directors List of directors
     * @return BoardOfDirectors object
     */
    public BoardOfDirectors toBoardOfDirectors(String symbol, List<BoardOfDirector> directors) {
        if (directors == null) {
            return null;
        }
        BaseModel baseModel = baseModelMapper.getBaseModel(symbol, "TradeB");
        return BoardOfDirectors.builder()
            .id(baseModel.getId())
            .symbol(baseModel.getSymbol())
            .source(baseModel.getSource())
            .audit(baseModel.getAudit())
            .directors(toDirectors(symbol, directors))
            .build();
    }

    public List<Director> toDirectors(String symbol,List<BoardOfDirector> directors) {
        if (directors == null) {
            return null;
        }
        return directors.stream()
        .map(director -> getDirector(symbol, director))
        .collect(Collectors.toList());
    }

    private Director getDirector(String symbol,BoardOfDirector director) {
        return Director.builder()
            .dirName(director.getDirectorName())
            .reportedDsg(director.getDesignation())
            .companyId(symbol)
            .directorType(director.getDirectorType())
            .build();
    }
}
