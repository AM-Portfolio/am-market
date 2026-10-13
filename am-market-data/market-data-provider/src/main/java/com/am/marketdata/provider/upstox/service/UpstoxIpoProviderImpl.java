package com.am.marketdata.provider.upstox.service;

import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.am.marketdata.common.provider.IpoDataProvider;
import com.am.marketdata.provider.upstox.client.UpstoxIpoClient;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoDetailsWrapperDto;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoListWrapperDto;
import com.am.marketdata.provider.upstox.mapper.UpstoxIpoDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Upstox implementation of {@link IpoDataProvider}.
 * Adapts third-party Upstox REST calls into unified ASRAX domain models using {@link UpstoxIpoDtoMapper}.
 */
@Slf4j
@Service("upstoxIpoProvider")
@RequiredArgsConstructor
public class UpstoxIpoProviderImpl implements IpoDataProvider {

    private static final String PROVIDER_NAME = "UPSTOX";

    private final UpstoxIpoClient upstoxIpoClient;
    private final UpstoxIpoDtoMapper mapper;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<AsraxIpoSummaryDto> getIpos(String status, String issueType, Integer pageNumber, Integer records) {
        log.debug("UpstoxIpoProvider: Fetching listing for status={}, issueType={}, page={}, records={}",
                status, issueType, pageNumber, records);
        UpstoxIpoListWrapperDto wrapper = upstoxIpoClient.fetchIpos(status, issueType, pageNumber, records);

        if (wrapper == null || wrapper.getData() == null || wrapper.getData().isEmpty()) {
            return Collections.emptyList();
        }

        return wrapper.getData().stream()
                .map(mapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AsraxIpoDetailsDto> getIpoDetails(String ipoId) {
        log.debug("UpstoxIpoProvider: Fetching full details for ipoId='{}'", ipoId);
        Optional<UpstoxIpoDetailsWrapperDto> wrapperOpt = upstoxIpoClient.fetchIpoDetails(ipoId);

        if (wrapperOpt.isEmpty() || wrapperOpt.get().getData() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(mapper.toDetailsDto(wrapperOpt.get().getData()));
    }
}
