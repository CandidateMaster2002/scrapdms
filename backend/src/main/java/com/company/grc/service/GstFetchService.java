package com.company.grc.service;

import com.company.grc.dto.DeepvueGstDto;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.service.DeepvueApiService.DeepvueApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
     * If not found, calls Deepvue API to fetch and store; on API error stores a stub with apiError=true.
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
    @Transactional
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
    private void mapApiDataToEntity(GstDetailsEntity entity, DeepvueGstDto.DataPayload data) {
        entity.setTradeName(data.getBusinessName());
        entity.setLegalName(data.getLegalName());
        entity.setGstType(data.getConstitutionOfBusiness());
        entity.setGstStatus(data.getGstinStatus());
        entity.setAggregateTurnover(data.getAnnualTurnover());
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

        // Count filing delays
        int delayGstr1 = 0, delayGstr3b = 0;
        if (data.getFilingStatus() != null) {
            for (List<DeepvueGstDto.FilingEntry> group : data.getFilingStatus()) {
                if (group == null) continue;
                for (DeepvueGstDto.FilingEntry entry : group) {
                    if (entry == null || entry.getStatus() == null) continue;
                    if ("GSTR1".equalsIgnoreCase(entry.getReturnType())
                            && !"Filed".equalsIgnoreCase(entry.getStatus())) {
                        delayGstr1++;
                    }
                    if ("GSTR3B".equalsIgnoreCase(entry.getReturnType())
                            && !"Filed".equalsIgnoreCase(entry.getStatus())) {
                        delayGstr3b++;
                    }
                }
            }
        }
        entity.setDelayCountGstr1(delayGstr1);
        entity.setDelayCountGstr3b(delayGstr3b);
    }
}
