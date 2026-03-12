package com.company.grc.service;

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
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class GrcCalculationService {

    private final GstFetchService gstFetchService;
    private final GrcRuleEngine ruleEngine;
    private final GrcScoreRepository grcScoreRepository;
    private final GstDetailsRepository gstDetailsRepository;

    @Autowired
    public GrcCalculationService(GstFetchService gstFetchService, GrcRuleEngine ruleEngine,
            GrcScoreRepository grcScoreRepository,
            GstDetailsRepository gstDetailsRepository) {
        this.gstFetchService = gstFetchService;
        this.ruleEngine = ruleEngine;
        this.grcScoreRepository = grcScoreRepository;
        this.gstDetailsRepository = gstDetailsRepository;
    }

    @Transactional
    public ApiDto.GrcResponse forceCalculateScore(String gstin) {
        GstDetailsEntity details = gstFetchService.fetchAndPersist(gstin);

        BigDecimal rawScore = ruleEngine.calculateScore(details);
        Integer score = rawScore.setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(gstin);
        String nextVersion = "v1";
        if (existingScoreOpt.isPresent()) {
            String currentVersion = existingScoreOpt.get().getScoreVersion();
            if (currentVersion != null && currentVersion.startsWith("v")) {
                try {
                    int dashIndex = currentVersion.indexOf('-');
                    String baseVersion = dashIndex > 0 ? currentVersion.substring(1, dashIndex)
                            : currentVersion.substring(1);
                    int versionNum = Integer.parseInt(baseVersion);
                    nextVersion = "v" + (versionNum + 1);
                } catch (NumberFormatException e) {
                    nextVersion = "v1";
                }
            }
        }

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .scoreVersion(nextVersion)
                .calculatedAt(LocalDateTime.now())
                .build();

        grcScoreRepository.save(scoreEntity);

        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(score)
                .scoreVersion(nextVersion)
                .calculatedAt(scoreEntity.getCalculatedAt())
                .build();
    }

    @Transactional
    public ApiDto.GrcResponse calculateScore(String gstin) {
        // 1. Fetch Data (DB or fresh from API if needed)
        GstDetailsEntity details = gstFetchService.getGstDetails(gstin);

        // 2. Check for existing fresh score
        // 2. Check for existing fresh score
        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(gstin);

        if (existingScoreOpt.isPresent()) {
            GrcScoreEntity existingScore = existingScoreOpt.get();
            // If details have not been updated since the last score calculation, return the
            // cached score.
            // details.getLastApiSync() check handles the "update on 11/21" requirement:
            // if details updated, sync time > score time, causing recalculation.
            if (details.getLastApiSync() != null && existingScore.getCalculatedAt().isAfter(details.getLastApiSync())) {
                return ApiDto.GrcResponse.builder()
                        .gstin(gstin)
                        .grcScore(existingScore.getScore())
                        .scoreVersion(existingScore.getScoreVersion())
                        .calculatedAt(existingScore.getCalculatedAt())
                        .build();
            }
        }

        // 3. Calculate Score
        BigDecimal rawScore = ruleEngine.calculateScore(details);
        Integer score = rawScore.setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        // 4. Derive version
        String nextVersion = "v1";
        if (existingScoreOpt.isPresent()) {
            String currentVersion = existingScoreOpt.get().getScoreVersion();
            if (currentVersion != null && currentVersion.startsWith("v")) {
                try {
                    int versionNum = Integer.parseInt(currentVersion.substring(1));
                    nextVersion = "v" + (versionNum + 1);
                } catch (NumberFormatException e) {
                    nextVersion = "v1";
                }
            }
        }

        // 5. Persist Score
        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .scoreVersion(nextVersion)
                .calculatedAt(LocalDateTime.now())
                .build();

        grcScoreRepository.save(scoreEntity);

        // 6. Return DTO
        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(score)
                .scoreVersion(nextVersion)
                .calculatedAt(scoreEntity.getCalculatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ApiDto.GstAppDetailsResponse getDetailsWithScore(String gstin) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found"));

        ApiDto.GstAppDetailsResponse.GstAppDetailsResponseBuilder builder = ApiDto.GstAppDetailsResponse.builder()
                .gstin(details.getGstin())
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
                    .scoreVersion(score.getScoreVersion())
                    .scoreCalculatedAt(score.getCalculatedAt());
        });

        return builder.build();
    }

    @Transactional(readOnly = true)
    public List<ApiDto.GstAppDetailsResponse> getAllDetailsWithScores() {
        return gstDetailsRepository.findAll().stream()
                .map(details -> getDetailsWithScore(details.getGstin()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiDto.GstAppDetailsResponse updateGstDetails(String gstin, ApiDto.GstDetailsUpdateRequest request) {
        GstDetailsEntity details = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found"));

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

        gstDetailsRepository.save(details);

        // Recalculate based on newly saved details
        recalculateStoredScore(gstin);

        return getDetailsWithScore(gstin);
    }

    @Transactional
    public ApiDto.GstAppDetailsResponse overrideGrcScore(String gstin, Integer newScore) {
        gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new RuntimeException("GSTIN not found. Cannot override score."));

        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(gstin);
        String nextVersion = "v1-manual";
        if (existingScoreOpt.isPresent()) {
            String currentVersion = existingScoreOpt.get().getScoreVersion();
            if (currentVersion != null && currentVersion.startsWith("v")) {
                try {
                    int dashIndex = currentVersion.indexOf('-');
                    String baseVersion = dashIndex > 0 ? currentVersion.substring(1, dashIndex)
                            : currentVersion.substring(1);
                    int versionNum = Integer.parseInt(baseVersion);
                    nextVersion = "v" + (versionNum + 1) + "-manual";
                } catch (NumberFormatException e) {
                    nextVersion = "v1-manual";
                }
            }
        }

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(newScore)
                .scoreVersion(nextVersion)
                .calculatedAt(LocalDateTime.now())
                .build();
        grcScoreRepository.save(scoreEntity);

        return getDetailsWithScore(gstin);
    }

    /**
     * Used by Scheduler
     */
    @Transactional
    public void recalculateStoredScore(String gstin) {
        // Force fetch from API done by caller (Scheduler) usually,
        // but here we just need to recalculate based on whatever is in DB
        // (assuming DB was just updated).

        // To stay pure to requirements, the scheduler will call
        // gstFetchService.fetchAndPersist(gstin) first
        // then call this.

        GstDetailsEntity details = gstFetchService.getGstDetails(gstin); // Will get fresh data if updated
        BigDecimal rawScore = ruleEngine.calculateScore(details);
        Integer score = rawScore.setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        Optional<GrcScoreEntity> existingScoreOpt = grcScoreRepository.findById(gstin);
        String nextVersion = "v1";
        if (existingScoreOpt.isPresent()) {
            String currentVersion = existingScoreOpt.get().getScoreVersion();
            if (currentVersion != null && currentVersion.startsWith("v")) {
                try {
                    int versionNum = Integer.parseInt(currentVersion.substring(1));
                    nextVersion = "v" + (versionNum + 1);
                } catch (NumberFormatException e) {
                    nextVersion = "v1";
                }
            }
        }

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .scoreVersion(nextVersion)
                .calculatedAt(LocalDateTime.now())
                .build();
        grcScoreRepository.save(scoreEntity);
    }

    @Transactional
    public void deleteGstDetails(String gstin) {
        // Delete score first due to foreign key constraints if any (or just logically cleaner)
        grcScoreRepository.deleteById(gstin);
        gstDetailsRepository.deleteById(gstin);
    }

    /**
     * Bulk recalculates scores for all GSTINs in the database.
     * This bypasses any caching since it's assumed rules/formula has changed.
     */
    @Transactional
    public void recalculateAll() {
        java.util.List<String> allGstins = gstDetailsRepository.findAllGstins();
        for (String gstin : allGstins) {
            recalculateStoredScore(gstin);
        }
    }
}
