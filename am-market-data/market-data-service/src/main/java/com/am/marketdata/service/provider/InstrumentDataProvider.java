package com.am.marketdata.service.provider;

import com.am.marketdata.service.dto.InstrumentSearchCriteria;
import java.io.IOException;
import java.util.List;

public interface InstrumentDataProvider {

    String getProviderName();

    void updateInstruments(String source) throws IOException;

    List<?> searchInstruments(InstrumentSearchCriteria criteria);
}
