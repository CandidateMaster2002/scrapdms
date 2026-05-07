package com.company.grc.service;

import com.company.grc.dto.DeepvueGstDto;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.service.DeepvueApiService.DeepvueApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class GstFetchService {

    private static final java.util.regex.Pattern GSTIN_PATTERN = java.util.regex.Pattern
            .compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private final GstDetailsRepository gstDetailsRepository;
    private final EmailService emailService;
    private final DeepvueApiService deepvueApiService;

    @Autowired
    public GstFetchService(GstDetailsRepository gstDetailsRepository,
            EmailService emailService,
            DeepvueApiService deepvueApiService) {
        this.gstDetailsRepository = gstDetailsRepository;
        this.emailService = emailService;
        this.deepvueApiService = deepvueApiService;
    }

    public void validateGstin(String gstin) {
        if (gstin == null || gstin.isBlank() || gstin.equals("0") || !GSTIN_PATTERN.matcher(gstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN supplied: [" + (gstin == null ? "null" : gstin) + "]");
        }
    }

    /**
     * Returns existing GST details from DB.
     * If not found, calls Deepvue API to fetch and store; on API error stores a
     * stub with apiError=true.
     */
    @Transactional
    public GstDetailsEntity getGstDetails(String gstin) {
        String trimmed = (gstin != null) ? gstin.trim() : null;
        validateGstin(trimmed);
        Optional<GstDetailsEntity> existing = gstDetailsRepository.findById(trimmed);
        if (existing.isPresent()) {
            return existing.get();
        }
        return fetchAndSaveFromApi(trimmed);
    }

    /**
     * Calls Deepvue API, maps response to entity and saves.
     * On API error, saves an error stub with apiError=true.
     */
    @Transactional
    public GstDetailsEntity fetchAndSaveFromApi(String gstin) {
        try {
            DeepvueGstDto.DataPayload data = deepvueApiService.fetchGstDetails(gstin);

            GstDetailsEntity entity = GstDetailsEntity.builder()
                    .gstin(gstin)
                    .source("API")
                    .dataSource("API")
                    .apiError(false)
                    .lastApiSync(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .delayCountGstr1(0)
                    .delayCountGstr3b(0)
                    .build();

            mapApiDataToEntity(entity, data);
            GstDetailsEntity saved = gstDetailsRepository.save(entity);
            emailService.sendNewGstNotification(gstin);
            return saved;

        } catch (Exception e) {
            System.err.println("fetchAndSaveFromApi failed for GSTIN " + gstin + ": " + e.getMessage());

            String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown API Error";
            if (errMsg.length() > 50) {
                errMsg = errMsg.substring(0, 47) + "...";
            }

            GstDetailsEntity errorEntity = GstDetailsEntity.builder()
                    .gstin(gstin)
                    .source(errMsg)
                    .dataSource("Error")
                    .apiError(true)
                    .lastApiSync(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .delayCountGstr1(0)
                    .delayCountGstr3b(0)
                    .build();
            return gstDetailsRepository.save(errorEntity);
        }
    }

    /**
     * Refreshes an existing GSTIN from the Deepvue API.
     * Used by admin-triggered refresh. Creates entity if it doesn't exist.
     * Throws RuntimeException if the API call fails (so caller can record error).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GstDetailsEntity refreshFromApi(String gstin) {
        GstDetailsEntity entity = gstDetailsRepository.findById(gstin)
                .orElseGet(() -> GstDetailsEntity.builder()
                        .gstin(gstin)
                        .createdAt(LocalDateTime.now())
                        .delayCountGstr1(0)
                        .delayCountGstr3b(0)
                        .build());
        try {
            DeepvueGstDto.DataPayload data = deepvueApiService.fetchGstDetails(gstin);
            mapApiDataToEntity(entity, data);
            entity.setSource("API");
            entity.setDataSource("API");
            entity.setApiError(false);
            entity.setLastApiSync(LocalDateTime.now());
            return gstDetailsRepository.save(entity);
        } catch (Exception e) {
            entity.setApiError(true);
            entity.setDataSource("Error");
            entity.setLastApiSync(LocalDateTime.now());
            gstDetailsRepository.save(entity);
            throw new RuntimeException("API error for GSTIN " + gstin + ": " + e.getMessage());
        }
    }

    /**
     * Maps Deepvue API data payload fields onto an existing entity instance.
     */
    private String cleanTurnover(String raw) {
        if (raw == null || raw.isBlank())
            return "Below 1 Cr.";
        String cleaned = raw.replaceFirst("(?i)^slab:\\s*", "")
                .replaceFirst("(?i)^Rs\\.\\s*", "")
                .trim();
        return cleaned.isBlank() ? "Below 1 Cr." : cleaned;
    }

    private void mapApiDataToEntity(GstDetailsEntity entity, DeepvueGstDto.DataPayload data) {
        entity.setTradeName(data.getBusinessName());
        entity.setLegalName(data.getLegalName());
        entity.setGstType(data.getConstitutionOfBusiness());
        entity.setGstStatus(data.getGstinStatus());
        entity.setAggregateTurnover(cleanTurnover(data.getAnnualTurnover()));
        entity.setPanNumber(data.getPanNumber());

        if (data.getDateOfRegistration() != null && !data.getDateOfRegistration().isBlank()
                && !data.getDateOfRegistration().startsWith("1800")) {
            try {
                entity.setRegistrationDate(LocalDate.parse(data.getDateOfRegistration()));
            } catch (Exception ignored) {
            }
        }

        if (data.getPromoters() != null && !data.getPromoters().isEmpty()) {
            entity.setPromoters(String.join(", ", data.getPromoters()).trim());
        }

        if (data.getContactDetails() != null && data.getContactDetails().getPrincipal() != null) {
            DeepvueGstDto.ContactEntry principal = data.getContactDetails().getPrincipal();
            entity.setMobile(principal.getMobile());
            entity.setEmail(principal.getEmail());
            entity.setAddress(principal.getAddress());
        }

        // Count filing delays using date-based rule (see GSTR_DELAY_COUNT_RULE.md)
        calculateDelayCounts(entity, data);

        // Reset delays if turnover < 3 Cr
        double turnoverCr = parseTurnoverCr(entity.getAggregateTurnover());
        if (turnoverCr < 3.0) {
            entity.setDelayCountGstr1(0);
            entity.setDelayCountGstr3b(0);
        }
    }

    private double parseTurnoverCr(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        boolean isLakhs = raw.toLowerCase().contains("lakh");
        String[] tokens = raw.replaceAll("[^0-9.]", " ").trim().split("\\s+");
        double sum = 0;
        int count = 0;
        for (String t : tokens) {
            if (t.isBlank()) continue;
            try {
                double val = Double.parseDouble(t);
                sum += isLakhs ? val / 100.0 : val;
                count++;
            } catch (NumberFormatException ignored) {}
        }
        return count > 0 ? sum / count : 0;
    }

    /**
     * Counts GSTR-1 and GSTR-3B delays for each month from Jan 2025
     * (or registration month, whichever is later) through last completed month.
     *
     * A month is a delay only when:
     * 1. Today is strictly after the due date for that return type, AND
     * 2. The return was either not found in filing_status OR was filed after the
     * due date.
     *
     * GSTR-1 due: 11th of following month.
     * GSTR-3B due: 21st of following month (25 Oct for September tax period).
     */
    private YearMonth parseFilingMonth(String fy, String taxPeriod) {
        if (fy == null || taxPeriod == null) return null;
        try {
            String[] years = fy.split("-");
            int year1 = Integer.parseInt(years[0]);
            int year2 = Integer.parseInt(years[1]);
            
            int month = java.time.Month.valueOf(taxPeriod.toUpperCase(Locale.ENGLISH)).getValue();
            int year = (month >= 4) ? year1 : year2;
            return YearMonth.of(year, month);
        } catch (Exception e) {
            return null;
        }
    }

    private void calculateDelayCounts(GstDetailsEntity entity, DeepvueGstDto.DataPayload data) {
        // Build lookup: "RETURNTYPE|FY|TaxPeriodMonthName" -> FilingEntry (include FY)
        Map<String, DeepvueGstDto.FilingEntry> filingMap = new HashMap<>();
        YearMonth earliestFilingMonth = null;
        
        if (data.getFilingStatus() != null) {
            for (List<DeepvueGstDto.FilingEntry> group : data.getFilingStatus()) {
                if (group == null)
                    continue;
                for (DeepvueGstDto.FilingEntry entry : group) {
                    if (entry == null)
                        continue;
                    if (entry.getReturnType() != null) {
                        String key = entry.getReturnType().toUpperCase()
                                + "|" + entry.getFinancialYear()
                                + "|" + entry.getTaxPeriod();
                        filingMap.put(key, entry);
                    }
                    
                    YearMonth ym = parseFilingMonth(entry.getFinancialYear(), entry.getTaxPeriod());
                    if (ym != null) {
                        if (earliestFilingMonth == null || ym.isBefore(earliestFilingMonth)) {
                            earliestFilingMonth = ym;
                        }
                    }
                }
            }
        }

        LocalDate today = LocalDate.now();

        // latest month logic: if current date is 12 or later -> last month, else last to last month
        YearMonth latestMonth;
        if (today.getDayOfMonth() >= 12) {
            latestMonth = YearMonth.from(today).minusMonths(1);
        } else {
            latestMonth = YearMonth.from(today).minusMonths(2);
        }

        // 12 months back from latest month (inclusive of latestMonth)
        YearMonth twelveMonthsBack = latestMonth.minusMonths(11);
        
        YearMonth start = twelveMonthsBack;
        if (earliestFilingMonth != null && earliestFilingMonth.isAfter(start)) {
            start = earliestFilingMonth;
        }

        int delayGstr1 = 0, delayGstr3b = 0;

        for (YearMonth ym = start; !ym.isAfter(latestMonth); ym = ym.plusMonths(1)) {
            int month = ym.getMonthValue();
            int year = ym.getYear();

            // Financial year: April starts new FY
            String fy = (month >= 4)
                    ? year + "-" + (year + 1)
                    : (year - 1) + "-" + year;

            String taxPeriod = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            YearMonth nextYm = ym.plusMonths(1);

            // ── GSTR-1 ──────────────────────────────────────────────
            LocalDate gstr1Due = LocalDate.of(nextYm.getYear(), nextYm.getMonth(), 11);
            if (today.isAfter(gstr1Due)) {
                String key = "GSTR1|" + fy + "|" + taxPeriod;
                DeepvueGstDto.FilingEntry entry = filingMap.get(key);
                // Missing record = Missed/Delayed
                if (entry == null || isFiledLate(entry, gstr1Due)) {
                    delayGstr1++;
                }
            }

            // ── GSTR-3B ─────────────────────────────────────────────
            // September tax period: due 25 Oct; all others: 21st of next month
            int gstr3bDay = (month == 9) ? 25 : 21;
            LocalDate gstr3bDue = LocalDate.of(nextYm.getYear(), nextYm.getMonth(), gstr3bDay);
            if (today.isAfter(gstr3bDue)) {
                String key = "GSTR3B|" + fy + "|" + taxPeriod;
                DeepvueGstDto.FilingEntry entry = filingMap.get(key);
                // Missing record = Missed/Delayed
                if (entry == null || isFiledLate(entry, gstr3bDue)) {
                    delayGstr3b++;
                }
            }
        }

        entity.setDelayCountGstr1(delayGstr1);
        entity.setDelayCountGstr3b(delayGstr3b);
    }

    /**
     * Returns true if the entry's date_of_filing is missing or strictly after
     * dueDate.
     */
    private boolean isFiledLate(DeepvueGstDto.FilingEntry entry, LocalDate dueDate) {
        if (entry.getDateOfFiling() == null || entry.getDateOfFiling().isBlank()) {
            return true;
        }
        try {
            return LocalDate.parse(entry.getDateOfFiling()).isAfter(dueDate);
        } catch (Exception e) {
            return true; // unparseable date → treat as not filed
        }
    }
}
