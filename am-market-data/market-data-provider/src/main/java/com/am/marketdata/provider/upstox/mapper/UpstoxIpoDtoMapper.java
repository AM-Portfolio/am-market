package com.am.marketdata.provider.upstox.mapper;

import com.am.marketdata.common.model.ipo.AsraxInvestorCategoryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoRegistrarDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoTimelineDto;
import com.am.marketdata.provider.upstox.dto.UpstoxInvestorCategoryDto;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoDetailsResponseDto;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoItemResponseDto;
import com.am.marketdata.provider.upstox.dto.UpstoxRegistrarInfoDto;
import com.am.marketdata.provider.upstox.dto.UpstoxTimelineDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component converting raw Upstox DTO payloads into unified ASRAX domain objects.
 * Implements null-safe mappings to handle optional/missing fields safely.
 */
@Component
public class UpstoxIpoDtoMapper {

    /**
     * Maps raw Upstox IPO item summary to AsraxIpoSummaryDto.
     */
    public AsraxIpoSummaryDto toSummaryDto(UpstoxIpoItemResponseDto src) {
        if (src == null) {
            return null;
        }

        return AsraxIpoSummaryDto.builder()
                .id(src.getId())
                .symbol(src.getSymbol())
                .companyName(src.getName())
                .status(src.getStatus())
                .isin(src.getIsin())
                .issueType(src.getIssueType())
                .issueSizeCr(src.getIssueSize())
                .industry(src.getIndustry())
                .minimumPrice(src.getMinimumPrice())
                .maximumPrice(src.getMaximumPrice())
                .biddingStartDate(src.getBiddingStartDate())
                .biddingEndDate(src.getBiddingEndDate())
                .totalSubscription(src.getTotalSubscription())
                .eligibleInvestors(mapInvestors(src.getInvestors()))
                .build();
    }

    /**
     * Maps raw Upstox full detail payload to AsraxIpoDetailsDto.
     */
    public AsraxIpoDetailsDto toDetailsDto(UpstoxIpoDetailsResponseDto src) {
        if (src == null) {
            return null;
        }

        return AsraxIpoDetailsDto.builder()
                .id(src.getId())
                .symbol(src.getSymbol())
                .companyName(src.getName())
                .status(src.getStatus())
                .isin(src.getIsin())
                .issueType(src.getIssueType())
                .issueSizeCr(src.getIssueSize())
                .industry(src.getIndustry())
                .minimumPrice(src.getMinimumPrice())
                .maximumPrice(src.getMaximumPrice())
                .biddingStartDate(src.getBiddingStartDate())
                .biddingEndDate(src.getBiddingEndDate())
                .dailyStartTime(src.getDailyStartTime())
                .dailyEndTime(src.getDailyEndTime())
                .faceValue(src.getFaceValue())
                .tickSize(src.getTickSize())
                .lotSize(src.getLotSize())
                .minimumQuantity(src.getMinimumQuantity())
                .cutOffPrice(src.getCutOffPrice())
                .listingPrice(src.getListingPrice())
                .listingExchange(src.getListingExchange())
                .rhpUrl(src.getRhpUrl())
                .drhpUrl(src.getDrhpUrl())
                .timeline(mapTimeline(src.getTimeline()))
                .registrarInfo(mapRegistrar(src.getRegistrarInfo()))
                .totalSubscription(src.getTotalSubscription())
                .eligibleInvestors(mapInvestors(src.getInvestors()))
                .build();
    }

    public AsraxIpoTimelineDto mapTimeline(UpstoxTimelineDto src) {
        if (src == null) {
            return null;
        }
        return AsraxIpoTimelineDto.builder()
                .preApplyStartDate(src.getPreApplyStartDate())
                .applicationStartDate(src.getApplicationStartDate())
                .applicationEndDate(src.getApplicationEndDate())
                .allotmentStartDate(src.getAllotmentStartDate())
                .allotmentDate(src.getAllotmentDate())
                .refundInitiationDate(src.getRefundInitiationDate())
                .listingDate(src.getListingDate())
                .mandateEndDate(src.getMandateEndDate())
                .build();
    }

    public AsraxIpoRegistrarDto mapRegistrar(UpstoxRegistrarInfoDto src) {
        if (src == null) {
            return null;
        }
        return AsraxIpoRegistrarDto.builder()
                .name(src.getName())
                .email(src.getEmail())
                .contactName(src.getContactName())
                .contactNumber(src.getContactNumber())
                .website(src.getWebsite())
                .registrarKey(src.getRegistrar())
                .build();
    }

    public List<AsraxInvestorCategoryDto> mapInvestors(List<UpstoxInvestorCategoryDto> srcList) {
        if (srcList == null || srcList.isEmpty()) {
            return Collections.emptyList();
        }
        return srcList.stream()
                .map(item -> AsraxInvestorCategoryDto.builder()
                        .category(item.getCategory())
                        .description(item.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
