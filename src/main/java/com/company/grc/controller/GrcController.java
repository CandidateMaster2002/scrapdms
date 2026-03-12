package com.company.grc.controller;

import com.company.grc.dto.ApiDto;
import com.company.grc.service.GrcCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/grc")
@CrossOrigin(origins = "*")
public class GrcController {

    private final GrcCalculationService grcCalculationService;

    @Autowired
    public GrcController(GrcCalculationService grcCalculationService) {
        this.grcCalculationService = grcCalculationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiDto.GrcResponse> calculateScore(@RequestBody ApiDto.GrcRequest request) {
        ApiDto.GrcResponse response = grcCalculationService.calculateScore(request.getGstin());
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
            grcCalculationService.forceCalculateScore(gstin);
        }

        return ResponseEntity.ok("Successfully triggered manual fetch from external API for "
                + request.getGstins().size() + " GSTIN(s).");
    }

    @DeleteMapping("/details/{gstin}")
    public ResponseEntity<String> deleteGstDetails(@PathVariable String gstin) {
        grcCalculationService.deleteGstDetails(gstin);
        return ResponseEntity.ok("Successfully deleted details for GSTIN: " + gstin);
    }
}
