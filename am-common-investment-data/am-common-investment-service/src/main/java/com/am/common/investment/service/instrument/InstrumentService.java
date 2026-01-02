package com.am.common.investment.service.instrument;

import com.am.common.investment.model.equity.Instrument;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing instrument data
 */
public interface InstrumentService {
    /**
     * Save an instrument to the database
     * 
     * @param instrument the instrument to save
     * @param symbol the symbol associated with the instrument
     * @return the saved instrument
     */
    Instrument saveInstrument(Instrument instrument, String symbol);
    
    /**
     * Get an instrument by symbol
     * 
     * @param symbol the symbol to look up
     * @return the instrument if found
     */
    Optional<Instrument> getInstrumentBySymbol(String symbol);
    
    /**
     * Get an instrument by trading symbol
     * 
     * @param tradingSymbol the trading symbol to look up
     * @return the instrument if found
     */
    Optional<Instrument> getInstrumentByTradingSymbol(String tradingSymbol);
    
    /**
     * Get an instrument by ISIN
     * 
     * @param isin the ISIN to look up
     * @return the instrument if found
     */
    Optional<Instrument> getInstrumentByIsin(String isin);
    
    /**
     * Get all instruments for a specific exchange
     * 
     * @param exchange the exchange to filter by
     * @return list of instruments for the exchange
     */
    List<Instrument> getInstrumentsByExchange(String exchange);

    /**
     * Get an instrument by instrument token
     * 
     * @param instrumentToken the instrument token to look up
     * @return the instrument if found
     */
    Optional<Instrument> getInstrumentByInstrumentToken(Long instrumentToken);
    
    /**
     * Delete an instrument by symbol
     * 
     * @param symbol the symbol of the instrument to delete
     */
    void deleteInstrumentBySymbol(String symbol);
    
    /**
     * Save multiple instruments to the database in batch
     * 
     * @param instruments list of instruments to save
     * @return list of saved instruments
     */
    List<Instrument> saveAll(List<Instrument> instruments);
    
    /**
     * Get instruments by multiple trading symbols
     * 
     * @param tradingSymbols the list of trading symbols to look up
     * @return list of instruments matching the provided trading symbols
     */
    List<Instrument> getInstrumentByTradingsymbols(List<String> tradingSymbols);

        /**
     * Get instruments by multiple trading symbols
     * 
     * @param tradingSymbols the list of trading symbols to look up
     * @return list of instruments matching the provided trading symbols
     */
    List<Instrument> getInstrumentByInstrumentTokens(List<Long> instrumentTokens);
}
