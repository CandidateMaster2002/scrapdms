package com.company.grc.controller;

import com.company.grc.dto.ApiDto;
import com.company.grc.entity.GrcRuleConfigEntity;
import com.company.grc.service.GrcCalculationService;
import com.company.grc.service.GrcRuleConfigService;
import com.company.grc.service.GstFetchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grc")
@CrossOrigin(origins = "*")
public class GrcController {

    private final GrcCalculationService grcCalculationService;
    private final GrcRuleConfigService ruleConfigService;
    private final GstFetchService gstFetchService;

    @Autowired
    public GrcController(GrcCalculationService grcCalculationService,
            GrcRuleConfigService ruleConfigService,
            GstFetchService gstFetchService) {
        this.grcCalculationService = grcCalculationService;
        this.ruleConfigService = ruleConfigService;
        this.gstFetchService = gstFetchService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiDto.GrcResponse> calculateScore(@RequestBody ApiDto.GrcRequest request) {
        // Validation moved to service, but we trim here for consistency
        String gstin = request.getGstin() != null ? request.getGstin().trim() : null;
        gstFetchService.validateGstin(gstin);
        ApiDto.GrcResponse response = grcCalculationService.calculateScore(gstin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recalculate/{gstin}")
    public ResponseEntity<ApiDto.GrcResponse> recalculateScore(@PathVariable String gstin) {
        ApiDto.GrcResponse response = grcCalculationService.forceCalculateScore(gstin);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recalculate-all")
    public ResponseEntity<String> recalculateAll() {
        grcCalculationService.recalculateAll();
        return ResponseEntity.ok("Recalculation triggered for all GST records.");
    }

    @GetMapping("/details/{gstin}")
    public ResponseEntity<ApiDto.GstAppDetailsResponse> getGstDetailsWithScore(@PathVariable String gstin) {
        return ResponseEntity.ok(grcCalculationService.getDetailsWithScore(gstin));
    }

    @GetMapping("/details")
    public ResponseEntity<List<ApiDto.GstAppDetailsResponse>> getAllGstDetailsWithScores() {
        return ResponseEntity.ok(grcCalculationService.getAllDetailsWithScores());
    }

    @PutMapping("/details/{gstin}")
    public ResponseEntity<ApiDto.GstAppDetailsResponse> updateGstDetails(@PathVariable String gstin,
            @RequestBody ApiDto.GstDetailsUpdateRequest request) {
        return ResponseEntity.ok(grcCalculationService.updateGstDetails(gstin, request));
    }

    @PutMapping("/score/{gstin}")
    public ResponseEntity<ApiDto.GstAppDetailsResponse> overrideGrcScore(@PathVariable String gstin,
            @RequestBody ApiDto.GrcScoreOverrideRequest request) {
        return ResponseEntity.ok(grcCalculationService.overrideGrcScore(gstin, request.getNewScore()));
    }

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchGstDetails(@RequestBody ApiDto.GstFetchRequest request) {
        if (request.getGstins() == null || request.getGstins().isEmpty()) {
            return ResponseEntity.badRequest().body("Must provide at least one GSTIN in the 'gstins' array.");
        }

        for (String gstin : request.getGstins()) {
            if (gstin != null)
                gstin = gstin.trim();
            // Validate GSTIN using central service logic
            try {
                gstFetchService.validateGstin(gstin);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }

            // Creates a stub entry with default score if GSTIN is new,
            // or recalculates score from existing DB values if GSTIN already exists.
            grcCalculationService.calculateScore(gstin);
        }

        return ResponseEntity.ok("Processed " + request.getGstins().size()
                + " GSTIN(s). New entries created with default score; existing entries left unchanged.");
    }

    @DeleteMapping("/details/{gstin}")
    public ResponseEntity<String> deleteGstDetails(@PathVariable String gstin) {
        grcCalculationService.deleteGstDetails(gstin);
        return ResponseEntity.ok("Successfully deleted details for GSTIN: " + gstin);
    }

    /**
     * Permanent cleanup of "garbage" records.
     * Deletes records where GSTIN is clearly invalid.
     */
    @DeleteMapping("/cleanup-garbage")
    public ResponseEntity<String> cleanupGarbageRecords() {
        int count = grcCalculationService.cleanupInvalidRecords();
        return ResponseEntity.ok("Successfully removed " + count + " invalid/garbage records.");
    }

    // ── Rule Config Endpoints ─────────────────────────────────────────────

    /** Returns all configurable rule parameters with their current values. */
    @GetMapping("/rule-config")
    public ResponseEntity<List<GrcRuleConfigEntity>> getRuleConfig() {
        return ResponseEntity.ok(ruleConfigService.getAllConfig());
    }

    /**
     * Updates rule config values. Body: { "TYPE_MAX": 10.0, "TYPE_PROPR_MULT": 1.0,
     * ... }
     */
    @PutMapping("/rule-config")
    public ResponseEntity<List<GrcRuleConfigEntity>> updateRuleConfig(@RequestBody Map<String, Double> updates) {
        return ResponseEntity.ok(ruleConfigService.saveConfig(updates));
    }
}
