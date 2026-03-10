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
            return existing.get();
        }

        return fetchAndPersist(gstin);
    }

    @Transactional
    public GstDetailsEntity fetchAndPersist(String gstin) {
        ExternalGstDto.ApiResponse apiResponse = gstApiClient.fetchTaxpayerDetails(gstin);

        if (apiResponse == null || apiResponse.getData() == null) {
            throw new RuntimeException("No data found for GSTIN: " + gstin +
                    (apiResponse != null ? " (Message: " + apiResponse.getMessage() + ")" : ""));
        }

        ExternalGstDto.DataPayload data = apiResponse.getData();

        GstDetailsEntity entity = mapToEntity(data);
        return gstDetailsRepository.save(entity);
    }

    private GstDetailsEntity mapToEntity(ExternalGstDto.DataPayload data) {
        ExternalGstDto.TaxpayerDetails td = data.getTaxpayerDetails();

        String address = (td != null && td.getPradr() != null) ? td.getPradr().getAdr() : null;

        int delayCountGstr1 = calculateDelayCount(data.getTaxpayerReturnDetails(), "GSTR1", 11);
        int delayCountGstr3b = calculateDelayCount(data.getTaxpayerReturnDetails(), "GSTR3B", 20);

        GstDetailsEntity entity = GstDetailsEntity.builder()
                .gstin(td.getGstin())
                .gstType(td.getCtb())
                .tradeName(td.getTradeNam())
                .legalName(td.getLgnm())
                .registrationDate(parseDate(td.getRgdt()))
                .gstStatus(td.getSts())
                .address(address)
                .aggregateTurnover(td.getAggreTurnOver())
                .delayCountGstr1(delayCountGstr1)
                .delayCountGstr3b(delayCountGstr3b)
                .lastApiSync(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return entity;
    }

    private int calculateDelayCount(ExternalGstDto.TaxpayerReturnDetails returnDetails, String returnType,
            int expectedDueDay) {
        if (returnDetails == null || returnDetails.getFilingStatus() == null) {
            return 0;
        }

        int delayCount = 0;
        DateTimeFormatter dofFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (ExternalGstDto.FilingStatus status : returnDetails.getFilingStatus()) {
            if (returnType.equalsIgnoreCase(status.getRtntype()) && "Filed".equalsIgnoreCase(status.getStatus())) {
                try {
                    LocalDate dateOfFiling = LocalDate.parse(status.getDof(), dofFormatter);
                    String taxp = status.getTaxp();
                    int month = getMonthNumber(taxp);
                    if (month > 0) {
                        String fy = status.getFy(); // "2025-2026"
                        int year = Integer.parseInt(fy.substring(0, 4));
                        if (month <= 3) {
                            year++; // Jan, Feb, Mar belong to the second part of FY
                        }

                        LocalDate dueDate = LocalDate.of(year, month, 1).plusMonths(1).withDayOfMonth(expectedDueDay);
                        if (dateOfFiling.isAfter(dueDate)) {
                            delayCount++;
                        }
                    }
                } catch (Exception e) {
                    // ignore parse errors
                }
            }
        }
        return delayCount;
    }

    private int getMonthNumber(String monthName) {
        if (monthName == null)
            return 0;
        switch (monthName.toLowerCase()) {
            case "january":
                return 1;
            case "february":
                return 2;
            case "march":
                return 3;
            case "april":
                return 4;
            case "may":
                return 5;
            case "june":
                return 6;
            case "july":
                return 7;
            case "august":
                return 8;
            case "september":
                return 9;
            case "october":
                return 10;
            case "november":
                return 11;
            case "december":
                return 12;
            default:
                return 0;
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return null; // Handle gracefully
        }
    }
}
