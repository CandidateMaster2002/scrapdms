package com.company.grc.scheduler;

import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.service.GrcCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MonthlyGstSyncScheduler {

    private final GstDetailsRepository gstDetailsRepository;
    private final GrcCalculationService grcCalculationService;

    @Autowired
    public MonthlyGstSyncScheduler(GstDetailsRepository gstDetailsRepository,
            GrcCalculationService grcCalculationService) {
        this.gstDetailsRepository = gstDetailsRepository;
        this.grcCalculationService = grcCalculationService;
    }

    @Scheduled(cron = "${gst.sync.cron:0 0 0 11,21 * ?}")
    public void runBiMonthlySync() {
        log.info("Starting Bi-Monthly GST Sync...");

        // Optimize: Fetch only necessary IDs (GSTINs) to save memory
        List<String> allGstins = gstDetailsRepository.findAllGstins();

        // Optimize: Use parallel stream for concurrent processing
        // Note: For very large datasets, consider using a custom ExecutorService or
        // batch processing
        allGstins.parallelStream().forEach(gstin -> {
            try {
                // External API removed — recalculate score based on current DB values only.
                // Users update GST details manually through the update endpoint.
                grcCalculationService.recalculateStoredScore(gstin);

                log.info("Successfully recalculated score for GSTIN: {}", gstin);
            } catch (Exception e) {
                log.error("Failed to recalculate score for GSTIN: {}", gstin, e);
            }
        });

        log.info("Bi-Monthly GST Sync Completed.");
    }
}
