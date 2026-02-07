package com.company.grc.scheduler;

import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.service.GrcCalculationService;
import com.company.grc.service.GstFetchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MonthlyGstSyncScheduler {

    private final GstDetailsRepository gstDetailsRepository;
    private final GstFetchService gstFetchService;
    private final GrcCalculationService grcCalculationService;

    @Autowired
    public MonthlyGstSyncScheduler(GstDetailsRepository gstDetailsRepository,
            GstFetchService gstFetchService,
            GrcCalculationService grcCalculationService) {
        this.gstDetailsRepository = gstDetailsRepository;
        this.gstFetchService = gstFetchService;
        this.grcCalculationService = grcCalculationService;
    }

    @Scheduled(cron = "0 0 0 11,21 * ?")
    public void runBiMonthlySync() {
        log.info("Starting Bi-Monthly GST Sync...");

        // Optimize: Fetch only necessary IDs (GSTINs) to save memory
        List<String> allGstins = gstDetailsRepository.findAllGstins();

        // Optimize: Use parallel stream for concurrent processing
        // Note: For very large datasets, consider using a custom ExecutorService or
        // batch processing
        allGstins.parallelStream().forEach(gstin -> {
            try {
                // 1. Force update from API
                gstFetchService.fetchAndPersist(gstin);

                // 2. Recalculate Score
                grcCalculationService.recalculateStoredScore(gstin);

                log.info("Successfully synced and recalculated for GSTIN: {}", gstin);
            } catch (Exception e) {
                log.error("Failed to sync for GSTIN: {}", gstin, e);
            }
        });

        log.info("Bi-Monthly GST Sync Completed.");
    }
}
