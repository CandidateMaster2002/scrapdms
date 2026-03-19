package com.company.grc.service;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GstDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles fetching/creating GstDetailsEntity records.
 *
 * External API (Kashidigital) has been REMOVED.
 * New GSTINs are added with empty/default values and the user fills in details manually.
 */

@Service
public class GstFetchService {

    // GSTIN validation pattern (15‑character Indian GST number)
    private static final java.util.regex.Pattern GSTIN_PATTERN =
            java.util.regex.Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private void validateGstin(String gstin) {
        if (gstin == null || gstin.isBlank() || gstin.equals("0") || !GSTIN_PATTERN.matcher(gstin).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN supplied: " + gstin);
        }
    }

    private final GstDetailsRepository gstDetailsRepository;

    @Autowired
    public GstFetchService(GstDetailsRepository gstDetailsRepository) {
        this.gstDetailsRepository = gstDetailsRepository;
    }

    /**
     * Returns existing GST details from DB.
     * If not found, creates a stub record with all empty values.
     */
    @Transactional
    public GstDetailsEntity getGstDetails(String gstin) {
        // Validate GSTIN format before any DB operation
        validateGstin(gstin);
        Optional<GstDetailsEntity> existing = gstDetailsRepository.findById(gstin);
        return existing.orElseGet(() -> createStubEntry(gstin));
    }

    /**
     * Creates a minimal stub entry for a new GSTIN with all fields empty/null.
     * The user will fill in the details via the update API.
     */
    @Transactional
    public GstDetailsEntity createStubEntry(String gstin) {
        // If already exists, return it (idempotent)
        Optional<GstDetailsEntity> existing = gstDetailsRepository.findById(gstin);
        if (existing.isPresent()) {
            return existing.get();
        }

        GstDetailsEntity stub = GstDetailsEntity.builder()
                .gstin(gstin)
                .gstType(null)
                .tradeName(null)
                .legalName(null)
                .registrationDate(null)
                .gstStatus(null)
                .address(null)
                .aggregateTurnover(null)
                .delayCountGstr1(0)
                .delayCountGstr3b(0)
                .source("From API")
                .lastApiSync(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        return gstDetailsRepository.save(stub);
    }
}
