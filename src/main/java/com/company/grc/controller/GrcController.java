package com.company.grc.controller;

import com.company.grc.dto.ApiDto;
import com.company.grc.entity.GrcRuleConfigEntity;
import com.company.grc.service.GrcCalculationService;
import com.company.grc.service.GrcRuleConfigService;
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

    @Autowired
    public GrcController(GrcCalculationService grcCalculationService, GrcRuleConfigService ruleConfigService) {
        this.grcCalculationService = grcCalculationService;
        this.ruleConfigService = ruleConfigService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiDto.GrcResponse> calculateScore(@RequestBody ApiDto.GrcRequest request) {
        ApiDto.GrcResponse response = grcCalculationService.calculateScore(request.getGstin());
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
            // Validate GSTIN format before any processing
            if (gstin == null || gstin.isBlank() || gstin.equals("0") || !gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$")) {
                return ResponseEntity.badRequest().body("Invalid GSTIN supplied: " + gstin);
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

    // ── Rule Config Endpoints ─────────────────────────────────────────────

    /** Returns all configurable rule parameters with their current values. */
    @GetMapping("/rule-config")
    public ResponseEntity<List<GrcRuleConfigEntity>> getRuleConfig() {
        return ResponseEntity.ok(ruleConfigService.getAllConfig());
    }

    /** Updates rule config values. Body: { "TYPE_MAX": 10.0, "TYPE_PROPR_MULT": 1.0, ... } */
    @PutMapping("/rule-config")
    public ResponseEntity<List<GrcRuleConfigEntity>> updateRuleConfig(@RequestBody Map<String, Double> updates) {
        return ResponseEntity.ok(ruleConfigService.saveConfig(updates));
    }
}
