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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
     * 1. Check if a GRC score row already exists.
     * 2a. If it exists and is NOT a DUMMY — return the existing score (no
     * recalculation).
     * 2b. If it exists and IS a DUMMY — the user hasn't edited details yet; return
     * dummy.
     * 3. If no score exists → create stub GST details + insert DUMMY score (15).
     */
    @Transactional
    public ApiDto.GrcResponse calculateScore(String gstin) {
        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(gstin);

        if (existingScoreOpt.isPresent()) {
            GrcScoreEntity existingScore = existingScoreOpt.get();
            return ApiDto.GrcResponse.builder()
                    .gstin(gstin)
                    .grcScore(existingScore.getScore())
                    .calculatedAt(existingScore.getCalculatedAt())
                    .build();
        }

        // New GSTIN — create stub GST details with empty values
        gstFetchService.createStubEntry(gstin);

        // Insert dummy score
        GrcScoreEntity dummyScore = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(config.DUMMY_DEFAULT_SCORE)
                .calculatedAt(LocalDateTime.now())
                .updatedBy("Dummy")
                .build();
        grcScoreRepository.save(dummyScore);

        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(config.DUMMY_DEFAULT_SCORE)
                .calculatedAt(dummyScore.getCalculatedAt())
                .build();
    }

    /**
     * Forces a recalculation of the score for an existing GSTIN based on current DB
     * values.
     * Used by the /fetch endpoint and bulk recalculate.
     */
    @Transactional
    public ApiDto.GrcResponse forceCalculateScore(String gstin) {
        // Ensure the GSTIN exists (creates stub if not)
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

    @Transactional(readOnly = true)
    public ApiDto.GstAppDetailsResponse getDetailsWithScore(GstDetailsEntity details) {
        return getDetailsWithScore(details, false);
    }

    private ApiDto.GstAppDetailsResponse getDetailsWithScore(GstDetailsEntity details, boolean includeBreakdown) {
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
                .delayCountGstr3b(details.getDelayCountGstr3b());

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
        return getDetailsWithScore(details, true);
    }

    @Transactional(readOnly = true)
    public List<ApiDto.GstAppDetailsResponse> getAllDetailsWithScores() {
        List<GstDetailsEntity> allDetails = gstDetailsRepository.findAll();
        // Batch fetch all scores into memory to avoid N+1 findById queries
        Map<String, GrcScoreEntity> scoreMap = grcScoreRepository.findAll().stream()
                .collect(Collectors.toMap(GrcScoreEntity::getGstin, s -> s));

        return allDetails.stream()
                .map(details -> {
                    ApiDto.GstAppDetailsResponse.GstAppDetailsResponseBuilder builder = ApiDto.GstAppDetailsResponse.builder()
                            .gstin(details.getGstin())
                            .tradeName(details.getTradeName())
                            .legalName(details.getLegalName())
                            .gstStatus(details.getGstStatus())
                            .delayCountGstr1(details.getDelayCountGstr1())
                            .delayCountGstr3b(details.getDelayCountGstr3b());

                    GrcScoreEntity score = scoreMap.get(details.getGstin());
                    if (score != null) {
                        builder.grcScore(score.getScore())
                                .scoreCalculatedAt(score.getCalculatedAt())
                                .updatedBy(score.getUpdatedBy());
                    }
                    
                    // Fields needed for Quick Edit view toggle
                    builder.registrationDate(details.getRegistrationDate())
                           .aggregateTurnover(details.getAggregateTurnover())
                           .gstType(details.getGstType())
                           .address(details.getAddress());
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiDto.GstAppDetailsResponse updateGstDetails(String gstin, ApiDto.GstDetailsUpdateRequest request) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found: " + gstin));

        if (request.getGstType() != null)
            details.setGstType(request.getGstType());
        if (request.getTradeName() != null)
            details.setTradeName(request.getTradeName());
        if (request.getLegalName() != null)
            details.setLegalName(request.getLegalName());
        if (request.getRegistrationDate() != null)
            details.setRegistrationDate(request.getRegistrationDate());
        if (request.getGstStatus() != null)
            details.setGstStatus(request.getGstStatus());
        if (request.getAddress() != null)
            details.setAddress(request.getAddress());
        if (request.getAggregateTurnover() != null)
            details.setAggregateTurnover(request.getAggregateTurnover());
        if (request.getDelayCountGstr1() != null)
            details.setDelayCountGstr1(request.getDelayCountGstr1());
        if (request.getDelayCountGstr3b() != null)
            details.setDelayCountGstr3b(request.getDelayCountGstr3b());

        // Mark as user-updated
        details.setLastApiSync(LocalDateTime.now());
        gstDetailsRepository.save(details);

        // Recalculate score based on newly saved details
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
                .updatedBy("SUPER_ADMIN_MANUAL")
                .build();
        grcScoreRepository.save(scoreEntity);

        return getDetailsWithScore(gstin);
    }

    /**
     * Recalculates and persists the GRC score for a given GSTIN using current DB
     * values.
     * Increments the version number (or initialises to v1 if DUMMY_VALUE).
     */
    @Transactional
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

    /**
     * Bulk recalculates scores for all GSTINs in the database.
     */
    @Transactional
    public void recalculateAll() {
        List<String> allGstins = gstDetailsRepository.findAllGstins();
        for (String gstin : allGstins) {
            recalculateStoredScore(gstin);
        }
    }
}
