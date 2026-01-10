package com.company.grc.service;

import com.company.grc.dto.ApiDto;
import com.company.grc.entity.GrcScoreEntity;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GrcScoreRepository;
import com.company.grc.rule.GrcRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class GrcCalculationService {

    private final GstFetchService gstFetchService;
    private final GrcRuleEngine ruleEngine;
    private final GrcScoreRepository grcScoreRepository;

    @Autowired
    public GrcCalculationService(GstFetchService gstFetchService, GrcRuleEngine ruleEngine,
            GrcScoreRepository grcScoreRepository) {
        this.gstFetchService = gstFetchService;
        this.ruleEngine = ruleEngine;
        this.grcScoreRepository = grcScoreRepository;
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
        BigDecimal score = ruleEngine.calculateScore(details);

        // 4. Persist Score
        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .scoreVersion("v1")
                .calculatedAt(LocalDateTime.now())
                .build();

        grcScoreRepository.save(scoreEntity);

        // 5. Return DTO
        return ApiDto.GrcResponse.builder()
                .gstin(gstin)
                .grcScore(score)
                .scoreVersion("v1")
                .calculatedAt(scoreEntity.getCalculatedAt())
                .build();
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
        BigDecimal score = ruleEngine.calculateScore(details);

        GrcScoreEntity scoreEntity = GrcScoreEntity.builder()
                .gstin(gstin)
                .score(score)
                .scoreVersion("v1")
                .calculatedAt(LocalDateTime.now())
                .build();
        grcScoreRepository.save(scoreEntity);
    }
}
