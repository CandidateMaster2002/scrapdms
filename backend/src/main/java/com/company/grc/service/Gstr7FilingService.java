package com.company.grc.service;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.entity.Gstr7FilingDetailEntity;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.repository.Gstr7FilingDetailRepository;
import com.company.grc.repository.Gstr7ReviewRepository;
import com.company.grc.entity.Gstr7ReviewEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class Gstr7FilingService {

    private final GeminiService geminiService;
    private final Gstr7FilingDetailRepository filingDetailRepository;
    private final GstDetailsRepository gstDetailsRepository;
    private final com.company.grc.repository.PanHsnConfigRepository panHsnConfigRepository;
    private final Gstr7ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record FilingPreviewItem(
            String returnPeriod,
            String returnPeriodLabel,
            String dueDate,
            String dateOfFiling,
            String status,
            int delayDays
    ) {}

    public record FilingPreviewResponse(
            List<FilingPreviewItem> items,
            String summaryStatus,
            int delayCount,
            int missedCount
    ) {}

    /**
     * Calls Gemini to parse pasted text, then calculates due dates, status, delay days.
     * Returns preview data — nothing is saved yet.
     */
    public FilingPreviewResponse parseAndPreview(String gstin, String tableText) {
        List<GeminiService.ParsedRecord> parsed = geminiService.parseFilingTable(tableText);
        YearMonth earliest = parsed.stream()
                .map(r -> {
                    try { return YearMonth.parse(r.returnPeriod()); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        List<YearMonth> relevant = getRelevantPeriods(earliest);
        
        // Convert parsed records to items, filtering by relevant periods
        List<FilingPreviewItem> items = parsed.stream()
                .filter(r -> {
                    try { return relevant.contains(YearMonth.parse(r.returnPeriod())); }
                    catch (Exception e) { return false; }
                })
                .map(this::toPreviewItem)
                .toList();

        Map<YearMonth, FilingPreviewItem> itemMap = items.stream()
                .collect(Collectors.toMap(i -> YearMonth.parse(i.returnPeriod()), i -> i));

        // month-1 is optional before the 11th: save if provided, but don't mark as Missed if absent
        YearMonth optionalPeriod = getOptionalPeriod();

        // Build the final list; skip optional period if it was not in the pasted data
        List<FilingPreviewItem> finalItems = relevant.stream()
                .filter(p -> itemMap.containsKey(p) || !p.equals(optionalPeriod))
                .map(p -> {
                    if (itemMap.containsKey(p)) return itemMap.get(p);
                    String label = p.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + p.getYear();
                    return new FilingPreviewItem(p.toString(), label, null, null, "Missed", 0);
                })
                .sorted(Comparator.comparing((FilingPreviewItem i) -> YearMonth.parse(i.returnPeriod())).reversed())
                .toList();

        // Count delayed and missed from the finalItems list
        long delayed = finalItems.stream().filter(i -> "Regular with Delay".equals(i.status())).count();
        long missed = finalItems.stream().filter(i -> "Missed".equals(i.status())).count();

        String status = "Regular without delay";
        if (missed > 0) status = "Missed";
        else if (delayed > 0) status = "Regular with Delay";
        
        boolean allMissing = finalItems.stream().allMatch(i -> i.dateOfFiling() == null && "Missed".equals(i.status()));
        if (allMissing) status = "Processing";

        return new FilingPreviewResponse(finalItems, status, (int) delayed, (int) missed);
    }

    /**
     * Parse and immediately save in background (async)
     */
    @org.springframework.scheduling.annotation.Async
    public void parseAndSaveAsync(String gstin, String tableText, String role, String username) {
        try {
            List<GeminiService.ParsedRecord> parsed = geminiService.parseFilingTable(tableText);
            if (parsed == null || parsed.isEmpty()) return;
            if ("user".equals(role)) {
                submitForReview(gstin, parsed, username);
            } else {
                saveFilingDetails(gstin, parsed);
            }
        } catch (Exception e) {
            System.err.println("Async parse and save failed for GSTIN " + gstin + ": " + e.getMessage());
        }
    }

    /**
     * Saves confirmed filing records, then updates gstr7_delay_count on gst_details.
     */
    @Transactional
    public void saveFilingDetails(String gstin, List<GeminiService.ParsedRecord> records) {
        filingDetailRepository.deleteByGstin(gstin);
        filingDetailRepository.flush(); // Ensure delete is executed before inserts

        YearMonth earliest = records.stream()
                .map(r -> {
                    try { return YearMonth.parse(r.returnPeriod()); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        List<YearMonth> relevant = getRelevantPeriods(earliest);

        // Filter by relevant periods AND de-duplicate by returnPeriod
        Map<String, GeminiService.ParsedRecord> uniqueRecords = new HashMap<>();
        for (GeminiService.ParsedRecord r : records) {
            try {
                YearMonth ym = YearMonth.parse(r.returnPeriod());
                if (relevant.contains(ym)) {
                    // If duplicate, keep the one with a filing date if the current one doesn't have one
                    if (!uniqueRecords.containsKey(r.returnPeriod()) || 
                        (uniqueRecords.get(r.returnPeriod()).dateOfFiling() == null && r.dateOfFiling() != null)) {
                        uniqueRecords.put(r.returnPeriod(), r);
                    }
                }
            } catch (Exception ignored) {}
        }

        YearMonth optionalPeriod = getOptionalPeriod();

        for (YearMonth p : relevant) {
            String periodStr = p.toString();
            GeminiService.ParsedRecord rec = uniqueRecords.get(periodStr);

            if (rec == null) {
                // Data not provided in paste
                if (p.equals(optionalPeriod)) {
                    continue; // optional period is not counted as missed
                }
                // Mandatory period is missing -> save explicitly as Missed
                LocalDate dueDate = calculateDueDate(periodStr);
                Gstr7FilingDetailEntity entity = Gstr7FilingDetailEntity.builder()
                        .gstin(gstin)
                        .returnPeriod(periodStr)
                        .build();
                entity.setDueDate(dueDate);
                entity.setDateOfFiling(null);
                entity.setStatus("Missed");
                entity.setDelayDays(0);
                filingDetailRepository.save(entity);
            } else {
                // Data was provided
                LocalDate dueDate = calculateDueDate(rec.returnPeriod());
                LocalDate filingDate = rec.dateOfFiling() != null && !rec.dateOfFiling().isBlank()
                        ? LocalDate.parse(rec.dateOfFiling())
                        : null;
                String status = deriveStatus(filingDate, dueDate);
                int delayDays = deriveDelayDays(filingDate, dueDate);

                Gstr7FilingDetailEntity entity = Gstr7FilingDetailEntity.builder()
                        .gstin(gstin)
                        .returnPeriod(rec.returnPeriod())
                        .build();

                entity.setDueDate(dueDate);
                entity.setDateOfFiling(filingDate);
                entity.setStatus(status);
                entity.setDelayDays(delayDays);
                filingDetailRepository.save(entity);
            }
        }

        updateGstDetailsAggregate(gstin);
    }

    @Transactional(readOnly = true)
    public List<Gstr7FilingDetailEntity> getFilingDetails(String gstin) {
        return filingDetailRepository.findByGstinOrderByReturnPeriodDesc(gstin);
    }

    @Transactional(readOnly = true)
    public List<Gstr7FilingDetailEntity> getAllFilingDetails() {
        return filingDetailRepository.findAll();
    }

    // ── Review Workflow ─────────────────────────────────────────────────────

    @Transactional
    public void submitForReview(String gstin, List<GeminiService.ParsedRecord> records, String submittedBy) {
        try {
            String json = objectMapper.writeValueAsString(records);
            Gstr7ReviewEntity review = Gstr7ReviewEntity.builder()
                    .gstin(gstin)
                    .submittedBy(submittedBy)
                    .submittedAt(java.time.LocalDateTime.now())
                    .parsedData(json)
                    .status("PENDING")
                    .build();
            reviewRepository.save(review);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize records for review", e);
        }
    }

    public record ReviewResponse(
            Long id,
            String gstin,
            String companyName,
            String gstdNo,
            String submittedBy,
            java.time.LocalDateTime submittedAt,
            String status,
            List<GeminiService.ParsedRecord> records,
            String summaryStatus,
            int delayCount,
            int missedCount,
            List<FilingPreviewItem> previewItems,
            String gstStatus,
            Integer delayCountGstr1,
            Integer delayCountGstr3b,
            java.time.LocalDateTime lastApiSync,
            String aggregateTurnover
    ) {}

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByStatusOrderBySubmittedAtDesc("PENDING")
                .stream().map(r -> {
                    List<GeminiService.ParsedRecord> records;
                    try {
                        records = objectMapper.readValue(r.getParsedData(),
                                objectMapper.getTypeFactory().constructCollectionType(List.class, GeminiService.ParsedRecord.class));
                    } catch (Exception e) {
                        records = List.of();
                    }

                    // Compute preview stats from the stored records
                    YearMonth earliest = records.stream()
                            .map(rec -> { try { return YearMonth.parse(rec.returnPeriod()); } catch (Exception e) { return null; } })
                            .filter(Objects::nonNull)
                            .min(Comparator.naturalOrder()).orElse(null);
                    List<YearMonth> relevant = getRelevantPeriods(earliest);
                    List<FilingPreviewItem> previewItems = records.stream()
                            .filter(rec -> { try { return relevant.contains(YearMonth.parse(rec.returnPeriod())); } catch (Exception e) { return false; } })
                            .map(this::toPreviewItem).collect(Collectors.toList());
                    Map<YearMonth, FilingPreviewItem> itemMap = previewItems.stream()
                            .collect(Collectors.toMap(i -> YearMonth.parse(i.returnPeriod()), i -> i));
                    List<FilingPreviewItem> finalItems = relevant.stream()
                            .map(p -> itemMap.containsKey(p) ? itemMap.get(p)
                                    : new FilingPreviewItem(p.toString(),
                                            p.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + p.getYear(),
                                            null, null, "Missed", 0))
                            .sorted(Comparator.comparing((FilingPreviewItem i) -> YearMonth.parse(i.returnPeriod())).reversed())
                            .collect(Collectors.toList());
                    int delayed = (int) finalItems.stream().filter(i -> "Regular with Delay".equals(i.status())).count();
                    int missed = (int) finalItems.stream().filter(i -> "Missed".equals(i.status())).count();
                    String summaryStatus = missed > 0 ? "Missed" : delayed > 0 ? "Regular with Delay" : "Regular without delay";

                    GstDetailsEntity gst = gstDetailsRepository.findById(r.getGstin()).orElse(null);
                    String companyName = gst != null ? (gst.getTradeName() != null ? gst.getTradeName() : gst.getLegalName()) : null;
                    String gstdNo = gst != null ? gst.getGstdNo() : null;

                    return new ReviewResponse(r.getId(), r.getGstin(), companyName, gstdNo, r.getSubmittedBy(), r.getSubmittedAt(),
                            r.getStatus(), records, summaryStatus, delayed, missed, finalItems,
                            gst != null ? gst.getGstStatus() : null,
                            gst != null ? gst.getDelayCountGstr1() : null,
                            gst != null ? gst.getDelayCountGstr3b() : null,
                            gst != null ? gst.getLastApiSync() : null,
                            gst != null ? gst.getAggregateTurnover() : null
                    );
                }).collect(Collectors.toList());
    }

    @Transactional
    public void approveReview(Long reviewId, List<GeminiService.ParsedRecord> overrideRecords, 
                              String summaryStatus, Integer delayCount, Integer missedCount) {
        Gstr7ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus("APPROVED");
        reviewRepository.save(review);
        
        saveFilingDetails(review.getGstin(), overrideRecords);

        // Apply manual summary overrides if provided
        if (summaryStatus != null || delayCount != null || missedCount != null) {
            Optional<GstDetailsEntity> optGst = gstDetailsRepository.findById(review.getGstin());
            if (optGst.isPresent()) {
                GstDetailsEntity entity = optGst.get();
                if (summaryStatus != null) entity.setGstr7Status(summaryStatus);
                if (delayCount != null) entity.setGstr7DelayCount(delayCount);
                if (missedCount != null) entity.setGstr7MissedCount(missedCount);
                entity.setGstr7LastUpdated(java.time.LocalDateTime.now());
                gstDetailsRepository.save(entity);
            }
        }
    }

    @Transactional
    public void rejectReview(Long reviewId) {
        Gstr7ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus("REJECTED");
        reviewRepository.save(review);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private FilingPreviewItem toPreviewItem(GeminiService.ParsedRecord rec) {
        LocalDate dueDate = calculateDueDate(rec.returnPeriod());
        LocalDate filingDate = rec.dateOfFiling() != null && !rec.dateOfFiling().isBlank()
                ? LocalDate.parse(rec.dateOfFiling())
                : null;
        String status = deriveStatus(filingDate, dueDate);
        int delayDays = deriveDelayDays(filingDate, dueDate);

        YearMonth ym = YearMonth.parse(rec.returnPeriod());
        String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();

        return new FilingPreviewItem(
                rec.returnPeriod(),
                label,
                dueDate != null ? dueDate.toString() : null,
                rec.dateOfFiling(),
                status,
                delayDays
        );
    }

    private LocalDate calculateDueDate(String returnPeriod) {
        try {
            YearMonth ym = YearMonth.parse(returnPeriod);
            // GSTR-7 is due on the 11th of the following month
            return ym.plusMonths(1).atDay(11);
        } catch (Exception e) {
            return null;
        }
    }

    private String deriveStatus(LocalDate filingDate, LocalDate dueDate) {
        if (filingDate == null) return "Missed";
        if (dueDate != null && filingDate.isAfter(dueDate)) return "Regular with Delay";
        return "Regular without delay";
    }

    private int deriveDelayDays(LocalDate filingDate, LocalDate dueDate) {
        if (filingDate == null || dueDate == null || !filingDate.isAfter(dueDate)) return 0;
        return (int) ChronoUnit.DAYS.between(dueDate, filingDate);
    }

    private void updateGstDetailsAggregate(String gstin) {
        Optional<GstDetailsEntity> optGst = gstDetailsRepository.findById(gstin);
        if (optGst.isEmpty()) return;

        GstDetailsEntity entity = optGst.get();
        String pan = entity.getPanNumber();

        boolean isApplicable = panHsnConfigRepository.findById(pan != null ? pan : "")
                .map(cfg -> Boolean.TRUE.equals(cfg.getIsApplicable()))
                .orElse(false);

        if (!isApplicable) {
            entity.setGstr7Status("NA");
            entity.setGstr7DelayCount(null);
            entity.setGstr7MissedCount(null);
            entity.setGstr7LastUpdated(java.time.LocalDateTime.now());
            gstDetailsRepository.save(entity);
            return;
        }

        List<Gstr7FilingDetailEntity> records = filingDetailRepository.findByGstinOrderByReturnPeriodDesc(gstin);
        
        YearMonth earliest = records.stream()
                .map(r -> {
                    try { return YearMonth.parse(r.getReturnPeriod()); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        List<YearMonth> relevant = getRelevantPeriods(earliest);
        
        Set<YearMonth> dbPeriods = records.stream()
                .map(r -> YearMonth.parse(r.getReturnPeriod()))
                .collect(Collectors.toSet());

        // month-1 is optional before the 11th — absent data must NOT be counted as missing
        YearMonth optionalPeriod = getOptionalPeriod();

        long delayedCount = records.stream()
                .filter(r -> relevant.contains(YearMonth.parse(r.getReturnPeriod())))
                .filter(r -> "Regular with Delay".equals(r.getStatus()))
                .count();

        long explicitMissed = records.stream()
                .filter(r -> relevant.contains(YearMonth.parse(r.getReturnPeriod())))
                .filter(r -> "Missed".equals(r.getStatus()))
                .count();

        // Periods absent from DB: exclude the optional period (filing window still open)
        long missingCount = relevant.stream()
                .filter(p -> !dbPeriods.contains(p))
                .filter(p -> !p.equals(optionalPeriod))
                .count();
        long totalMissed = explicitMissed + missingCount;

        if (records.isEmpty()) {
            entity.setGstr7Status("Processing");
            entity.setGstr7DelayCount(null);
            entity.setGstr7MissedCount(null);
        } else {
            entity.setGstr7DelayCount((int) delayedCount);
            entity.setGstr7MissedCount((int) totalMissed);

            if (totalMissed > 0) {
                entity.setGstr7Status("Missed");
            } else if (delayedCount > 0) {
                entity.setGstr7Status("Regular with Delay");
            } else {
                entity.setGstr7Status("Regular without delay");
            }
        }

        entity.setGstr7LastUpdated(java.time.LocalDateTime.now());
        gstDetailsRepository.save(entity);
    }

    /**
     * Always returns up to month-1 as the latest period (12 months window).
     * Whether month-1 is "required" (i.e. counted as missing when absent) is
     * determined separately by getOptionalPeriod().
     */
    private List<YearMonth> getRelevantPeriods(YearMonth earliestFilingMonth) {
        LocalDate today = LocalDate.now();
        // Always go up to month-1; the optional-period check handles the before-11th case
        YearMonth latest = YearMonth.from(today.minusMonths(1));

        YearMonth start = latest.minusMonths(11);
        if (earliestFilingMonth != null && earliestFilingMonth.isAfter(start)) {
            start = earliestFilingMonth;
        }

        List<YearMonth> periods = new ArrayList<>();
        for (YearMonth ym = latest; !ym.isBefore(start); ym = ym.minusMonths(1)) {
            periods.add(ym);
        }
        return periods;
    }

    /**
     * Returns month-1 when today is on or before the 11th (filing window still open),
     * meaning that period's absence should NOT be counted as missed.
     * Returns null after the 11th (window closed, absence = missed).
     */
    private YearMonth getOptionalPeriod() {
        LocalDate today = LocalDate.now();
        return today.getDayOfMonth() <= 11 ? YearMonth.from(today.minusMonths(1)) : null;
    }
}
