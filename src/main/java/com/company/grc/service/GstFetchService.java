package com.company.grc.service;

import com.company.grc.dto.ExternalGstDto;
import com.company.grc.entity.GstDetailsEntity;

import com.company.grc.integration.GstApiClient;
import com.company.grc.repository.GstDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GstFetchService {

    private final GstDetailsRepository gstDetailsRepository;
    private final GstApiClient gstApiClient;

    @Autowired
    public GstFetchService(GstDetailsRepository gstDetailsRepository, GstApiClient gstApiClient) {
        this.gstDetailsRepository = gstDetailsRepository;
        this.gstApiClient = gstApiClient;
    }

    @Transactional
    public GstDetailsEntity getGstDetails(String gstin) {
        Optional<GstDetailsEntity> existing = gstDetailsRepository.findById(gstin);

        if (existing.isPresent()) {
            GstDetailsEntity entity = existing.get();
            // Freshness check can be added here (e.g., if lastSync > 10 days ago)
            // For now, prompt implies "If data is stale" -> we can define stale as older
            // than last scheduled run or just use scheduler logic.
            // Requirement: "If GSTIN exists and data is fresh -> use DB data"
            // Let's assume data is fresh if updated today. But for simplicity, we return
            // DB.
            // The Scheduler handles the "Stale" updates ideally.
            // If explicit stale logic needed on read:
            if (entity.getLastApiSync().isAfter(LocalDateTime.now().minusDays(1))) {
                return entity;
            }
        }

        return fetchAndPersist(gstin);
    }

    @Transactional
    public GstDetailsEntity fetchAndPersist(String gstin) {
        ExternalGstDto.ApiResponse apiResponse = gstApiClient.fetchTaxpayerDetails(gstin);
        ExternalGstDto.DataPayload data = apiResponse.getData();

        GstDetailsEntity entity = mapToEntity(data);
        return gstDetailsRepository.save(entity);
    }

    private GstDetailsEntity mapToEntity(ExternalGstDto.DataPayload data) {
        ExternalGstDto.TaxpayerDetails td = data.getTaxpayerDetails();
        ExternalGstDto.BusinessPlaces bp = data.getBusiness_places();
        ExternalGstDto.GstFilingDelaySummary delaySummary = (data.getTaxpayerReturnDetails() != null)
                ? data.getTaxpayerReturnDetails().getGst_filing_delay_summary()
                : null;

        String address = (bp != null && bp.getPradr() != null) ? bp.getPradr().getAdr() : null;

        GstDetailsEntity entity = GstDetailsEntity.builder()
                .gstin(td.getGstin())
                .gstType(td.getCtb())
                .tradeName(td.getTradeNam())
                .legalName(td.getLgnm())
                .registrationDate(parseDate(td.getRgdt()))
                .gstStatus(td.getSts())
                .address(address)
                .aggregateTurnover(td.getAggreTurnOver())
                .delayCountGstr1(delaySummary != null ? delaySummary.getGst_delay_count_GSTR1() : 0)
                .delayCountGstr3b(delaySummary != null ? delaySummary.getGst_delay_count_GSTR3B() : 0)
                .lastApiSync(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // Detailed returns mapping removed as per requirement
        // if (data.getTaxpayerReturnDetails() != null ...) logic deleted

        return entity;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return null; // Handle gracefully
        }
    }
}
