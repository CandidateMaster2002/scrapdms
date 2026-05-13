package com.company.grc.service;

import com.company.grc.config.GrcScoreConfig;
import com.company.grc.dto.ApiDto;
import com.company.grc.entity.GrcScoreEntity;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GrcScoreRepository;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.repository.PanHsnConfigRepository;
import com.company.grc.repository.HsnCategoryRepository;
import com.company.grc.repository.Gstr7FilingDetailRepository;
import com.company.grc.entity.PanHsnConfigEntity;
import com.company.grc.entity.HsnCategoryEntity;
import com.company.grc.rule.GrcRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Pageable;
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
    private final PanHsnConfigRepository panHsnConfigRepository;
    private final HsnCategoryRepository hsnCategoryRepository;
    private final Gstr7FilingDetailRepository gstr7FilingDetailRepository;

    @Autowired
    public GrcCalculationService(GstFetchService gstFetchService,
            GrcRuleEngine ruleEngine,
            GrcScoreRepository grcScoreRepository,
            GstDetailsRepository gstDetailsRepository,
            GrcScoreConfig config,
            PanHsnConfigRepository panHsnConfigRepository,
            HsnCategoryRepository hsnCategoryRepository,
            Gstr7FilingDetailRepository gstr7FilingDetailRepository) {
        this.gstFetchService = gstFetchService;
        this.ruleEngine = ruleEngine;
        this.grcScoreRepository = grcScoreRepository;
        this.gstDetailsRepository = gstDetailsRepository;
        this.config = config;
        this.panHsnConfigRepository = panHsnConfigRepository;
        this.hsnCategoryRepository = hsnCategoryRepository;
        this.gstr7FilingDetailRepository = gstr7FilingDetailRepository;
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
            String gstr7Status = gstDetailsRepository.findById(trimmedGstin)
                    .map(d -> buildGstr7FilingStatus(d.getGstr7Status(), d.getGstr7DelayCount(), d.getGstr7MissedCount()))
                    .orElse("NA");
            return ApiDto.GrcResponse.builder()
                    .gstin(trimmedGstin)
                    .grcScore(existingScore.getScore())
                    .calculatedAt(existingScore.getCalculatedAt())
                    .gstr7FilingStatus(gstr7Status)
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
                    .gstr7FilingStatus(buildGstr7FilingStatus(details.getGstr7Status(), details.getGstr7DelayCount(), details.getGstr7MissedCount()))
                    .build();
        }

        recalculateStoredScore(trimmedGstin);
        GrcScoreEntity scoreEntity = grcScoreRepository.findById(trimmedGstin)
                .orElseThrow(() -> new RuntimeException("Score not found after recalculation for: " + trimmedGstin));
        GstDetailsEntity freshDetails = gstDetailsRepository.findById(trimmedGstin).orElse(details);

        return ApiDto.GrcResponse.builder()
                .gstin(trimmedGstin)
                .grcScore(scoreEntity.getScore())
                .calculatedAt(scoreEntity.getCalculatedAt())
                .gstr7FilingStatus(buildGstr7FilingStatus(freshDetails.getGstr7Status(), freshDetails.getGstr7DelayCount(), freshDetails.getGstr7MissedCount()))
                .build();
    }

    @Transactional
    public ApiDto.GrcResponse forceCalculateScore(String gstin) {
        gstFetchService.getGstDetails(gstin);
        recalculateStoredScore(gstin);

        GrcScoreEntity scoreEntity = grcScoreRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("Score not found after recalculation for: " + gstin));
        GstDetailsEntity details = gstDetailsRepository.findById(gstin).orElse(null);

        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(scoreEntity.getScore())
                .calculatedAt(scoreEntity.getCalculatedAt())
                .gstr7FilingStatus(details != null
                        ? buildGstr7FilingStatus(details.getGstr7Status(), details.getGstr7DelayCount(), details.getGstr7MissedCount())
                        : "NA")
                .build();
    }

    /** Formats the human-readable GSTR-7 filing status string for API consumers. */
    private String buildGstr7FilingStatus(String status, Integer delayCount, Integer missedCount) {
        if (status == null || status.isBlank()) return "NA";
        return switch (status.trim()) {
            case "Regular without delay" -> "Regular without delay";
            case "Regular with Delay" -> {
                int d = (delayCount != null) ? delayCount : 0;
                yield "Regular with " + d + " delay" + (d == 1 ? "" : "s");
            }
            case "Missed" -> {
                int m = (missedCount != null) ? missedCount : 0;
                yield m + " missed";
            }
            case "Processing" -> "Processing";
            case "NA" -> "NA";
            default -> status;
        };
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
                .createdAt(details.getCreatedAt())
                .gstdNo(details.getGstdNo())
                .gstr7Status(details.getGstr7Status())
                .gstr7DelayCount(details.getGstr7DelayCount())
                .gstr7MissedCount(details.getGstr7MissedCount())
                .gstr7LastUpdated(details.getGstr7LastUpdated());

        gstr7FilingDetailRepository.findByGstinOrderByReturnPeriodDesc(gstin).stream().findFirst()
                .ifPresent(f -> builder.gstr7LastReturnPeriod(f.getReturnPeriod()));

        String pan = details.getPanNumber();
        if (pan != null) pan = pan.trim().toUpperCase();
        
        // Fallback to deriving PAN from GSTIN if missing or invalid
        if ((pan == null || pan.length() < 10) && gstin != null && gstin.length() >= 12) {
            pan = gstin.substring(2, 12).toUpperCase();
        }

        if (pan != null) {
            panHsnConfigRepository.findById(pan).ifPresent(config -> {
                if (config.getCategoryId() != null) {
                    hsnCategoryRepository.findById(config.getCategoryId()).ifPresent(cat -> {
                        builder.categoryName(cat.getName());
                    });
                }
            });
        }

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

        Map<String, PanHsnConfigEntity> panConfigMap = panHsnConfigRepository.findAll().stream()
                .collect(Collectors.toMap(PanHsnConfigEntity::getPan, c -> c));

        Map<Long, HsnCategoryEntity> categoryMap = hsnCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(HsnCategoryEntity::getId, c -> c));

        Map<String, String> lastReturnPeriodMap = gstr7FilingDetailRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        com.company.grc.entity.Gstr7FilingDetailEntity::getGstin,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(java.util.Comparator.comparing(com.company.grc.entity.Gstr7FilingDetailEntity::getReturnPeriod)),
                                opt -> opt.map(com.company.grc.entity.Gstr7FilingDetailEntity::getReturnPeriod).orElse(null)
                        )
                ));

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
                                    .createdAt(details.getCreatedAt())
                                    .gstdNo(details.getGstdNo())
                                    .gstr7Status(details.getGstr7Status())
                                    .gstr7DelayCount(details.getGstr7DelayCount())
                                    .gstr7MissedCount(details.getGstr7MissedCount())
                                    .gstr7LastUpdated(details.getGstr7LastUpdated())
                                    .gstr7LastReturnPeriod(lastReturnPeriodMap.get(details.getGstin()));

                    String pan = details.getPanNumber();
                    if (pan != null) pan = pan.trim().toUpperCase();
                    
                    if ((pan == null || pan.length() < 10) && details.getGstin() != null && details.getGstin().length() >= 12) {
                        pan = details.getGstin().substring(2, 12).toUpperCase();
                    }

                    if (pan != null) {
                        PanHsnConfigEntity config = panConfigMap.get(pan);
                        if (config != null && config.getCategoryId() != null) {
                            HsnCategoryEntity cat = categoryMap.get(config.getCategoryId());
                            if (cat != null) {
                                builder.categoryName(cat.getName());
                            }
                        }
                    }

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

    public record NewVendorItem(
            String gstin,
            String companyName,
            String gstStatus,
            String dataSource,
            String gstr7Status,
            String createdAt
    ) {}

    @Transactional(readOnly = true)
    public List<NewVendorItem> getNewVendors(Pageable pageable) {
        return gstDetailsRepository.findNewVendors(pageable).stream()
                .map(g -> new NewVendorItem(
                        g.getGstin(),
                        g.getLegalName() != null && !g.getLegalName().isBlank() ? g.getLegalName()
                                : g.getTradeName() != null ? g.getTradeName() : "",
                        g.getGstStatus(),
                        g.getDataSource(),
                        g.getGstr7Status(),
                        g.getCreatedAt() != null ? g.getCreatedAt().toString() : null
                ))
                .toList();
    }
}
