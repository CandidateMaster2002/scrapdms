package com.company.grc.controller;

import com.company.grc.entity.Gstr7FilingDetailEntity;
import com.company.grc.service.GeminiService;
import com.company.grc.service.Gstr7FilingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grc/admin/gstr7")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class Gstr7FilingController {

    private final Gstr7FilingService filingService;

    private boolean isNotAuthorized(String role) {
        return !"super_admin".equals(role) && !"user".equals(role);
    }

    /** Parse pasted table text via Gemini and return a preview — nothing saved yet. */
    @PostMapping("/parse-filing")
    public ResponseEntity<?> parseFiling(
            @RequestHeader(value = "Role", required = false) String role,
            @RequestBody ParseFilingRequest req) {
        if (isNotAuthorized(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        try {
            Gstr7FilingService.FilingPreviewResponse preview = filingService.parseAndPreview(req.getGstin(), req.getTableText());
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Parsing failed: " + e.getMessage());
        }
    }

    /** Trigger an asynchronous background parse and save. Returns immediately. */
    @PostMapping("/parse-save-async")
    public ResponseEntity<?> parseSaveAsync(
            @RequestHeader(value = "Role", required = false) String role,
            @RequestHeader(value = "Username", required = false, defaultValue = "user") String username,
            @RequestBody ParseFilingRequest req) {
        if (isNotAuthorized(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        
        filingService.parseAndSaveAsync(req.getGstin(), req.getTableText(), role, username);
        return ResponseEntity.ok(java.util.Map.of("status", "processing_in_background"));
    }

    /** Confirm and save parsed filing records for a GSTIN. */
    @PostMapping("/save-filing")
    public ResponseEntity<?> saveFiling(
            @RequestHeader(value = "Role", required = false) String role,
            @RequestHeader(value = "Username", required = false, defaultValue = "user") String username,
            @RequestBody SaveFilingRequest req) {
        if (isNotAuthorized(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        
        if ("user".equals(role)) {
            filingService.submitForReview(req.getGstin(), req.getRecords(), username);
            return ResponseEntity.ok(java.util.Map.of("status", "submitted_for_review"));
        } else {
            filingService.saveFilingDetails(req.getGstin(), req.getRecords());
            return ResponseEntity.ok(java.util.Map.of("status", "saved_directly"));
        }
    }

    /** Get stored filing details for a GSTIN. */
    @GetMapping("/filing-details/{gstin}")
    public ResponseEntity<?> getFilingDetails(
            @PathVariable String gstin,
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAuthorized(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        List<Gstr7FilingDetailEntity> details = filingService.getFilingDetails(gstin);
        return ResponseEntity.ok(details);
    }

    /** Bulk: Get all filing details for all GSTINs, grouped by GSTIN. */
    @GetMapping("/filing-details-all")
    public ResponseEntity<?> getAllFilingDetails(
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAuthorized(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        List<Gstr7FilingDetailEntity> all = filingService.getAllFilingDetails();
        // Group by GSTIN
        java.util.Map<String, List<Gstr7FilingDetailEntity>> grouped = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(Gstr7FilingDetailEntity::getGstin));
        return ResponseEntity.ok(grouped);
    }

    @Data
    public static class ParseFilingRequest {
        private String gstin;
        private String tableText;
    }

    @Data
    public static class SaveFilingRequest {
        private String gstin;
        private List<GeminiService.ParsedRecord> records;
        private String summaryStatus;
        private Integer delayCount;
        private Integer missedCount;
    }

    // ── Review Endpoints ────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(@RequestHeader(value = "Role", required = false) String role) {
        if (!"super_admin".equals(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        return ResponseEntity.ok(filingService.getPendingReviews());
    }

    @PostMapping("/reviews/{id}/approve")
    public ResponseEntity<?> approveReview(
            @RequestHeader(value = "Role", required = false) String role,
            @PathVariable Long id,
            @RequestBody SaveFilingRequest req) {
        if (!"super_admin".equals(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        filingService.approveReview(id, req.getRecords(), req.getSummaryStatus(), req.getDelayCount(), req.getMissedCount());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reviews/{id}/reject")
    public ResponseEntity<?> rejectReview(
            @RequestHeader(value = "Role", required = false) String role,
            @PathVariable Long id) {
        if (!"super_admin".equals(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        filingService.rejectReview(id);
        return ResponseEntity.ok().build();
    }
}
