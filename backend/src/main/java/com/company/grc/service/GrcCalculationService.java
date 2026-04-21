package com.company.grc.service;

import com.company.grc.config.GrcScoreConfig;
import com.company.grc.dto.ApiDto;
import com.company.grc.entity.GrcScoreEntity;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GrcScoreRepository;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.rule.GrcRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GrcCalculationService {

    private final GstFetchService gstFetchService;
    private final GrcRuleEngine ruleEngine;
    private final GrcScoreRepository grcScoreRepository;
    private final GstDetailsRepository gstDetailsRepository;
    private final GrcScoreConfig config;

    @Autowired
    public GrcCalculationService(GstFetchService gstFetchService,
            GrcRuleEngine ruleEngine,
            GrcScoreRepository grcScoreRepository,
            GstDetailsRepository gstDetailsRepository,
            GrcScoreConfig config) {
        this.gstFetchService = gstFetchService;
        this.ruleEngine = ruleEngine;
        this.grcScoreRepository = grcScoreRepository;
        this.gstDetailsRepository = gstDetailsRepository;
        this.config = config;
    }

    /**
     * Main entry point: get or create a GRC score for a GSTIN.
     *
     * Flow:
     * 1. If a GRC score already exists, return it immediately.
     * 2. If not: call Deepvue API via GstFetchService.getGstDetails().
     *    - API error → save score 15 (Error-Default).
     *    - API success → calculate and save real score.
     */
    @Transactional
    public ApiDto.GrcResponse calculateScore(String gstin) {
        String trimmedGstin = (gstin != null) ? gstin.trim() : null;
        gstFetchService.validateGstin(trimmedGstin);

        // Return existing score without re-calling API
        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(trimmedGstin);
        if (existingScoreOpt.isPresent()) {
            GrcScoreEntity existingScore = existingScoreOpt.get();
            return ApiDto.GrcResponse.builder()
                    .gstin(trimmedGstin)
                    .grcScore(existingScore.getScore())
                    .calculatedAt(existingScore.getCalculatedAt())
                    .build();
        }

        // New GSTIN — fetch from Deepvue API (creates error stub on failure)
        GstDetailsEntity details = gstFetchService.getGstDetails(trimmedGstin);

        if (Boolean.TRUE.equals(details.getApiError())) {
            GrcScoreEntity errorScore = GrcScoreEntity.builder()
                    .gstin(trimmedGstin)
                    .score(config.DUMMY_DEFAULT_SCORE)
                    .calculatedAt(LocalDateTime.now())
                    .updatedBy("Error-Default")
                    .build();
            grcScoreRepository.save(errorScore);
            return ApiDto.GrcResponse.builder()
                    .gstin(trimmedGstin)
                    .grcScore(config.DUMMY_DEFAULT_SCORE)
                    .calculatedAt(errorScore.getCalculatedAt())
                    .build();
        }

        recalculateStoredScore(trimmedGstin);
        GrcScoreEntity scoreEntity = grcScoreRepository.findById(trimmedGstin)
                .orElseThrow(() -> new RuntimeException("Score not found after recalculation for: " + trimmedGstin));

        return ApiDto.GrcResponse.builder()
                .gstin(trimmedGstin)
                .grcScore(scoreEntity.getScore())
                .calculatedAt(scoreEntity.getCalculatedAt())
                .build();
    }

    @Transactional
    public ApiDto.GrcResponse forceCalculateScore(String gstin) {
        gstFetchService.getGstDetails(gstin);
        recalculateStoredScore(gstin);

        GrcScoreEntity scoreEntity = grcScoreRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("Score not found after recalculation for: " + gstin));

        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(scoreEntity.getScore())
                .calculatedAt(scoreEntity.getCalculatedAt())
                .build();
    }

    // ── Detail retrieval ──────────────────────────────────────────────────────

    private ApiDto.GstAppDetailsResponse buildResponse(GstDetailsEntity details,
                                                        boolean includeBreakdown,
                                                        boolean includePrivate) {
        String gstin = details.getGstin();
        ApiDto.GstAppDetailsResponse.GstAppDetailsResponseBuilder builder = ApiDto.GstAppDetailsResponse.builder()
                .gstin(gstin)
                .gstType(details.getGstType())
                .tradeName(details.getTradeName())
                .legalName(details.getLegalName())
                .registrationDate(details.getRegistrationDate())
                .gstStatus(details.getGstStatus())
                .address(details.getAddress())
                .lastApiSync(details.getLastApiSync())
                .aggregateTurnover(details.getAggregateTurnover())
                .delayCountGstr1(details.getDelayCountGstr1())
                .delayCountGstr3b(details.getDelayCountGstr3b())
                .source(details.getSource())
                .apiError(details.getApiError())
                .dataSource(details.getDataSource())
                .panNumber(details.getPanNumber())
                .promoters(details.getPromoters())
                .createdAt(details.getCreatedAt());

        if (includePrivate) {
            builder.mobile(details.getMobile()).email(details.getEmail());
        }

        grcScoreRepository.findById(gstin).ifPresent(score -> {
            builder.grcScore(score.getScore())
                    .scoreCalculatedAt(score.getCalculatedAt())
                    .updatedBy(score.getUpdatedBy());
            if (includeBreakdown) {
                try {
                    builder.scoreBreakdown(ruleEngine.calculateBreakdown(details));
                } catch (Exception e) {
                    System.err.println("Error calculating breakdown for " + gstin + ": " + e.getMessage());
                }
            }
        });

        return builder.build();
    }

    @Transactional(readOnly = true)
    public ApiDto.GstAppDetailsResponse getDetailsWithScore(String gstin) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found: " + gstin));
        return buildResponse(details, true, false);
    }

    @Transactional(readOnly = true)
    public ApiDto.GstAppDetailsResponse getDetailsWithScoreAdmin(String gstin) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found: " + gstin));
        return buildResponse(details, true, true);
    }

    @Transactional(readOnly = true)
    public ApiDto.GstAppDetailsResponse getDetailsWithScore(GstDetailsEntity details) {
        return buildResponse(details, false, false);
    }

    @Transactional(readOnly = true)
    public List<ApiDto.GstAppDetailsResponse> getAllDetailsWithScores() {
        List<GstDetailsEntity> allDetails = gstDetailsRepository.findAll();
        Map<String, GrcScoreEntity> scoreMap = grcScoreRepository.findAll().stream()
                .collect(Collectors.toMap(GrcScoreEntity::getGstin, s -> s));

        return allDetails.stream()
                .map(details -> {
                    ApiDto.GstAppDetailsResponse.GstAppDetailsResponseBuilder builder =
                            ApiDto.GstAppDetailsResponse.builder()
                                    .gstin(details.getGstin())
                                    .tradeName(details.getTradeName())
                                    .legalName(details.getLegalName())
                                    .gstStatus(details.getGstStatus())
                                    .delayCountGstr1(details.getDelayCountGstr1())
                                    .delayCountGstr3b(details.getDelayCountGstr3b())
                                    .registrationDate(details.getRegistrationDate())
                                    .aggregateTurnover(details.getAggregateTurnover())
                                    .gstType(details.getGstType())
                                    .address(details.getAddress())
                                    .source(details.getSource())
                                    .apiError(details.getApiError())
                                    .dataSource(details.getDataSource())
                                    .panNumber(details.getPanNumber())
                                    .promoters(details.getPromoters())
                                    .mobile(details.getMobile())
                                    .email(details.getEmail())
                                    .createdAt(details.getCreatedAt());

                    GrcScoreEntity score = scoreMap.get(details.getGstin());
                    if (score != null) {
                        builder.grcScore(score.getScore())
                                .scoreCalculatedAt(score.getCalculatedAt())
                                .updatedBy(score.getUpdatedBy());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    // ── Update / Override ─────────────────────────────────────────────────────

    @Transactional
    public ApiDto.GstAppDetailsResponse updateGstDetails(String gstin, ApiDto.GstDetailsUpdateRequest request) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found: " + gstin));

        if (request.getGstType() != null) details.setGstType(request.getGstType());
        if (request.getTradeName() != null) details.setTradeName(request.getTradeName());
        if (request.getLegalName() != null) details.setLegalName(request.getLegalName());
        if (request.getRegistrationDate() != null) details.setRegistrationDate(request.getRegistrationDate());
        if (request.getGstStatus() != null) details.setGstStatus(request.getGstStatus());
        if (request.getAddress() != null) details.setAddress(request.getAddress());
        if (request.getAggregateTurnover() != null) details.setAggregateTurnover(request.getAggregateTurnover());
        if (request.getDelayCountGstr1() != null) details.setDelayCountGstr1(request.getDelayCountGstr1());
        if (request.getDelayCountGstr3b() != null) details.setDelayCountGstr3b(request.getDelayCountGstr3b());
        if (request.getMobile() != null) details.setMobile(request.getMobile());
        if (request.getEmail() != null) details.setEmail(request.getEmail());
        if (request.getPanNumber() != null) details.setPanNumber(request.getPanNumber());
        if (request.getPromoters() != null) details.setPromoters(request.getPromoters());

        // Mark as manual override — apiError is intentionally NOT cleared here.
        // If the API previously failed for this GSTIN it should remain flagged permanently
        // so bulk refresh continues to skip it (no wasted API quota).
        details.setSource("Manual");
        details.setDataSource("Manual");
        details.setLastApiSync(LocalDateTime.now());
        gstDetailsRepository.save(details);

        recalculateStoredScore(gstin, request.getUpdatedBy());
        return getDetailsWithScore(gstin);
    }

    @Transactional
    public ApiDto.GstAppDetailsResponse overrideGrcScore(String gstin, Integer newScore) {
        gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found. Cannot override score."));

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(newScore)
                .calculatedAt(LocalDateTime.now())
                .updatedBy("super_admin_manual")
                .build();
        grcScoreRepository.save(scoreEntity);
        return getDetailsWithScore(gstin);
    }

    // ── Admin Refresh from API ────────────────────────────────────────────────

    /**
     * Refreshes GSTIN data from Deepvue API.
     * If gstins is null/empty: refreshes all non-error GSTINs (bulk, skips error ones).
     * If gstins is provided: refreshes exactly those GSTINs (admin explicitly chose them, even errors).
     * Returns per-GSTIN result map.
     */
    @Transactional
    public Map<String, String> refreshFromApi(List<String> gstins, String updatedBy) {
        List<String> toRefresh;
        if (gstins == null || gstins.isEmpty()) {
            toRefresh = gstDetailsRepository.findByApiErrorFalseOrApiErrorIsNull()
                    .stream().map(GstDetailsEntity::getGstin).collect(Collectors.toList());
        } else {
            toRefresh = gstins.stream().map(String::trim).collect(Collectors.toList());
        }

        Map<String, String> results = new LinkedHashMap<>();
        for (String gstin : toRefresh) {
            try {
                gstFetchService.refreshFromApi(gstin);
                recalculateStoredScore(gstin, updatedBy);
                results.put(gstin, "refreshed");
            } catch (Exception e) {
                results.put(gstin, "error: " + e.getMessage());
            }
        }
        return results;
    }

    // ── Score calculation internals ───────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateStoredScore(String gstin, String updatedBy) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found: " + gstin));

        BigDecimal rawScore = ruleEngine.calculateScore(details);
        Integer score = rawScore.setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .calculatedAt(LocalDateTime.now())
                .updatedBy(updatedBy)
                .build();
        grcScoreRepository.save(scoreEntity);
    }

    @Transactional
    public void recalculateStoredScore(String gstin) {
        recalculateStoredScore(gstin, null);
    }

    @Transactional
    public void deleteGstDetails(String gstin) {
        grcScoreRepository.deleteById(gstin);
        gstDetailsRepository.deleteById(gstin);
    }

    @Transactional
    public void recalculateAll() {
        List<String> allGstins = gstDetailsRepository.findAllGstins();
        for (String gstin : allGstins) {
            recalculateStoredScore(gstin);
        }
    }

    @Transactional
    public int cleanupInvalidRecords() {
        List<String> allGstins = gstDetailsRepository.findAllGstins();
        int count = 0;
        for (String gstin : allGstins) {
            try {
                String trimmed = gstin != null ? gstin.trim() : null;
                gstFetchService.validateGstin(trimmed);
            } catch (IllegalArgumentException e) {
                deleteGstDetails(gstin);
                count++;
            }
        }
        return count;
    }
}
