package com.company.grc.service;

import com.company.grc.dto.PanGstr7DataResponse;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.entity.HsnCategoryEntity;
import com.company.grc.entity.PanHsnConfigEntity;
import com.company.grc.repository.GstDetailsRepository;
import com.company.grc.repository.HsnCategoryRepository;
import com.company.grc.repository.PanHsnConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Gstr7Service {

    private final PanHsnConfigRepository panHsnConfigRepository;
    private final GstDetailsRepository gstDetailsRepository;
    private final HsnCategoryRepository hsnCategoryRepository;

    @Transactional
    public PanHsnConfigEntity saveOrUpdateHsn(String pan, Long categoryId, String updatedBy) {
        PanHsnConfigEntity config = panHsnConfigRepository.findById(pan)
                .orElse(new PanHsnConfigEntity());
        config.setPan(pan);
        config.setCategoryId(categoryId);

        boolean isScrap = false;
        if (categoryId != null) {
            isScrap = hsnCategoryRepository.findById(categoryId)
                    .map(c -> "Scrap".equalsIgnoreCase(c.getName() != null ? c.getName().trim() : ""))
                    .orElse(false);
        }
        config.setIsApplicable(isScrap);

        config.setUpdatedBy(updatedBy);
        return panHsnConfigRepository.save(config);
    }

    @Transactional
    public GstDetailsEntity markGstinAsGstd(String gstin, String gstdNo) {
        GstDetailsEntity gstDetails = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new IllegalArgumentException("GSTIN not found: " + gstin));

        if (gstdNo != null && !gstdNo.trim().isEmpty()) {
            gstDetails.setGstType("GSTD");
            gstDetails.setGstdNo(gstdNo.trim());
        } else {
            if ("GSTD".equals(gstDetails.getGstType())) {
                gstDetails.setGstType(null);
            }
            gstDetails.setGstdNo(null);
        }

        return gstDetailsRepository.save(gstDetails);
    }

    @Transactional
    public GstDetailsEntity updateGstr7Data(String gstin, String status, Integer delayCount, Integer missedCount) {
        GstDetailsEntity gstDetails = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new IllegalArgumentException("GSTIN not found: " + gstin));

        gstDetails.setGstr7Status(status);
        if (delayCount != null) {
            gstDetails.setGstr7DelayCount(delayCount);
        }
        if (missedCount != null) {
            gstDetails.setGstr7MissedCount(missedCount);
        }
        gstDetails.setGstr7LastUpdated(LocalDateTime.now());

        return gstDetailsRepository.save(gstDetails);
    }

    @Transactional(readOnly = true)
    public List<PanGstr7DataResponse> fetchAllPanGstr7Data() {
        List<GstDetailsEntity> allGstDetails = gstDetailsRepository.findAll();

        Map<String, List<GstDetailsEntity>> groupedByPan = allGstDetails.stream()
                .filter(g -> g.getPanNumber() != null && !g.getPanNumber().trim().isEmpty())
                .collect(Collectors.groupingBy(GstDetailsEntity::getPanNumber));

        Map<Long, HsnCategoryEntity> categoriesById = hsnCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(HsnCategoryEntity::getId, c -> c));

        // Pre-fetch ALL PAN configs in one query instead of N individual queries
        Map<String, PanHsnConfigEntity> allConfigs = panHsnConfigRepository.findAll().stream()
                .collect(Collectors.toMap(PanHsnConfigEntity::getPan, c -> c));

        return groupedByPan.entrySet().stream()
                .map(entry -> {
                    String pan = entry.getKey();
                    PanHsnConfigEntity config = allConfigs.get(pan);
                    Long categoryId = config != null ? config.getCategoryId() : null;
                    HsnCategoryEntity category = categoryId != null ? categoriesById.get(categoryId) : null;

                    List<String> hsnCodes = category != null && category.getCodes() != null
                            ? category.getCodes().stream()
                                    .map(c -> c.getHsnCode())
                                    .collect(Collectors.toList())
                            : List.of();

                    List<PanGstr7DataResponse.GstinData> gstinDataList = entry.getValue().stream()
                            .map(g -> PanGstr7DataResponse.GstinData.builder()
                                    .gstin(g.getGstin())
                                    .gstr7Status(g.getGstr7Status())
                                    .gstr7DelayCount(g.getGstr7DelayCount())
                                    .gstr7MissedCount(g.getGstr7MissedCount())
                                    .gstr7LastUpdated(g.getGstr7LastUpdated())
                                    .gstType(g.getGstType())
                                    .gstdNo(g.getGstdNo())
                                    .tradeName(g.getTradeName())
                                    .legalName(g.getLegalName())
                                    .createdAt(g.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());

                    String companyName = entry.getValue().stream()
                            .filter(g -> g.getLegalName() != null && !g.getLegalName().trim().isEmpty())
                            .map(GstDetailsEntity::getLegalName)
                            .findFirst()
                            .orElse(entry.getValue().stream()
                                    .filter(g -> g.getTradeName() != null && !g.getTradeName().trim().isEmpty())
                                    .map(GstDetailsEntity::getTradeName)
                                    .findFirst()
                                    .orElse(""));

                    return PanGstr7DataResponse.builder()
                            .panNumber(pan)
                            .companyName(companyName)
                            .categoryId(categoryId)
                            .categoryName(category != null ? category.getName() : null)
                            .hsnCodes(hsnCodes)
                            .isApplicable(category != null && "Scrap".equalsIgnoreCase(category.getName() != null ? category.getName().trim() : ""))
                            .gstins(gstinDataList)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
