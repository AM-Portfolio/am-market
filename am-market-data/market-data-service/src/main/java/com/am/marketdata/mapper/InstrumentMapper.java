package com.am.marketdata.mapper;

import com.am.common.investment.model.equity.Instrument;
import com.am.common.investment.model.equity.Instrument.InstrumentType;
import com.am.common.investment.model.equity.Instrument.Segment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class to convert between Zerodha Instrument and AM Common Instrument models
 */
@Slf4j
@Component
public class InstrumentMapper {

    /**
     * Convert a Zerodha instrument to AM common instrument model
     *
     * @param zerodhaInstrument Zerodha instrument model
     * @return AM common instrument model
     */
    public Instrument toCommonInstrument(com.zerodhatech.models.Instrument zerodhaInstrument) {
        if (zerodhaInstrument == null) {
            return null;
        }

        try {
            Instrument instrument = new Instrument();
            
            // Map basic properties
            instrument.setTradingSymbol(zerodhaInstrument.tradingsymbol);
            instrument.setInstrumentToken(zerodhaInstrument.instrument_token);
            instrument.setName(zerodhaInstrument.name);
            instrument.setExchangeToken(zerodhaInstrument.exchange_token);
           
            // Map instrument type
            if (zerodhaInstrument.instrument_type != null) {
                instrument.setInstrumentType(mapInstrumentType(zerodhaInstrument.instrument_type));
            }
            
            // Map segment - assuming there's a method to set segment
            if (zerodhaInstrument.segment != null) {
                instrument.setSegment(mapSegment(zerodhaInstrument.segment));
            }
            
            // Map tick size and lot size
            if (zerodhaInstrument.tick_size > 0) {
                instrument.setTickSize(BigDecimal.valueOf(zerodhaInstrument.tick_size));
            }
            instrument.setLotSize(zerodhaInstrument.lot_size);
            
            // Map expiry if available
            if (zerodhaInstrument.expiry != null) {
                instrument.setExpiry(zerodhaInstrument.expiry);
            }
            
            // // Map strike if available
            // if (zerodhaInstrument.strike > 0) {
            //     instrument.setStrike(BigDecimal.valueOf(zerodhaInstrument.strike));
            // }
            
            return instrument;
        } catch (Exception e) {
            log.error("Error mapping Zerodha instrument to common instrument: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert a list of Zerodha instruments to a list of AM common instruments
     *
     * @param zerodhaInstruments List of Zerodha instrument models
     * @return List of AM common instrument models
     */
    public List<Instrument> toCommonInstruments(List<com.zerodhatech.models.Instrument> zerodhaInstruments) {
        if (zerodhaInstruments == null || zerodhaInstruments.isEmpty()) {
            return new ArrayList<>();
        }
        
        return zerodhaInstruments.stream()
                .map(this::toCommonInstrument)
                .filter(instrument -> instrument != null)
                .collect(Collectors.toList());
    }
    
    /**
     * Map Zerodha instrument type to AM common instrument type
     *
     * @param zerodhaType Zerodha instrument type
     * @return AM common instrument type
     */
    private InstrumentType mapInstrumentType(String zerodhaType) {
        if (zerodhaType == null) {
            return InstrumentType.UNKNOWN;
        }
        
        switch (zerodhaType.toUpperCase()) {
            case "EQ":
                return InstrumentType.EQUITY;
            case "FUT":
                return InstrumentType.FUTURE;
            case "OPT":
                return InstrumentType.UNKNOWN;
            case "CE":
                return InstrumentType.CALL_OPTION;
            case "PE":
                return InstrumentType.PUT_OPTION;
            case "IDX":
                return InstrumentType.INDEX;
            case "ETF":
                return InstrumentType.UNKNOWN;
            default:
                return InstrumentType.UNKNOWN;
        }
    }
    
    /**
     * Map Zerodha segment to AM common segment
     *
     * @param zerodhaSegment Zerodha segment
     * @return AM common segment
     */
    private Segment mapSegment(String zerodhaSegment) {
        if (zerodhaSegment == null) {
            return Segment.UNKNOWN;
        }
        
        switch (zerodhaSegment.toUpperCase()) {
            case "NSE":
                return Segment.NSE;
            case "BSE":
                return Segment.BSE;
            case "MCX_OPT":
                return Segment.MCX_OPT;
            case "NFO_FUT":
                return Segment.NFO_FUT;
            case "NCO_FUT":
                return Segment.NCO_FUT;
            case "NFO_OPT":
                return Segment.NFO_OPT;
            default:
                return Segment.UNKNOWN;
        }
    }

}
