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
        List<YearMonth> relevant = getRelevantPeriods();
        
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

        // Create a complete list of 12 months, filling in missing ones as Missed
        List<FilingPreviewItem> finalItems = relevant.stream()
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
     * Saves confirmed filing records, then updates gstr7_delay_count on gst_details.
     */
    @Transactional
    public void saveFilingDetails(String gstin, List<GeminiService.ParsedRecord> records) {
        filingDetailRepository.deleteByGstin(gstin);

        List<YearMonth> relevant = getRelevantPeriods();
        List<GeminiService.ParsedRecord> filtered = records.stream()
                .filter(r -> {
                    try {
                        return relevant.contains(YearMonth.parse(r.returnPeriod()));
                    } catch (Exception e) { return false; }
                })
                .toList();

        for (GeminiService.ParsedRecord rec : filtered) {
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

        updateGstDetailsAggregate(gstin);
    }

    @Transactional(readOnly = true)
    public List<Gstr7FilingDetailEntity> getFilingDetails(String gstin) {
        return filingDetailRepository.findByGstinOrderByReturnPeriodDesc(gstin);
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

    @Transactional(readOnly = true)
    public List<Gstr7ReviewEntity> getPendingReviews() {
        return reviewRepository.findByStatusOrderBySubmittedAtDesc("PENDING");
    }

    @Transactional
    public void approveReview(Long reviewId, List<GeminiService.ParsedRecord> overrideRecords) {
        Gstr7ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus("APPROVED");
        reviewRepository.save(review);
        
        saveFilingDetails(review.getGstin(), overrideRecords);
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

        List<YearMonth> relevant = getRelevantPeriods();
        List<Gstr7FilingDetailEntity> records = filingDetailRepository.findByGstinOrderByReturnPeriodDesc(gstin);
        
        Set<YearMonth> dbPeriods = records.stream()
                .map(r -> YearMonth.parse(r.getReturnPeriod()))
                .collect(Collectors.toSet());

        long delayedCount = records.stream()
                .filter(r -> relevant.contains(YearMonth.parse(r.getReturnPeriod())))
                .filter(r -> "Regular with Delay".equals(r.getStatus()))
                .count();

        long explicitMissed = records.stream()
                .filter(r -> relevant.contains(YearMonth.parse(r.getReturnPeriod())))
                .filter(r -> "Missed".equals(r.getStatus()))
                .count();

        long missingCount = relevant.stream().filter(p -> !dbPeriods.contains(p)).count();
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

    private List<YearMonth> getRelevantPeriods() {
        LocalDate today = LocalDate.now();
        YearMonth latest;
        // Logic: Till 11th, upto month-2. From 12th, upto month-1.
        if (today.getDayOfMonth() <= 11) {
            latest = YearMonth.from(today.minusMonths(2));
        } else {
            latest = YearMonth.from(today.minusMonths(1));
        }

        List<YearMonth> periods = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            periods.add(latest.minusMonths(i));
        }
        return periods;
    }
}
