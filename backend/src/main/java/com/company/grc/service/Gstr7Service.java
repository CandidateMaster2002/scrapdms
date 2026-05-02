package com.company.grc.service;

import com.company.grc.dto.PanGstr7DataResponse;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.entity.PanHsnConfigEntity;
import com.company.grc.repository.GstDetailsRepository;
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
    private final com.company.grc.repository.Gstr7HsnMasterRepository gstr7HsnMasterRepository;

    @Transactional
    public PanHsnConfigEntity saveOrUpdateHsn(String pan, String hsnCode, String updatedBy) {
        PanHsnConfigEntity config = panHsnConfigRepository.findById(pan)
                .orElse(new PanHsnConfigEntity());
        
        config.setPan(pan);
        config.setHsnCode(hsnCode);
        // Auto-derive is_applicable based on the master list
        boolean isApplicable = gstr7HsnMasterRepository.existsById(hsnCode != null ? hsnCode : "");
        config.setIsApplicable(isApplicable);
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
                gstDetails.setGstType(null); // Or set to a default value
            }
            gstDetails.setGstdNo(null);
        }
        
        return gstDetailsRepository.save(gstDetails);
    }

    @Transactional
    public GstDetailsEntity updateGstr7Data(String gstin, String status, Integer delayCount) {
        GstDetailsEntity gstDetails = gstDetailsRepository.findById(gstin)
                .orElseThrow(() -> new IllegalArgumentException("GSTIN not found: " + gstin));
                
        gstDetails.setGstr7Status(status);
        if (delayCount != null) {
            gstDetails.setGstr7DelayCount(delayCount);
        }
        
        // Auto-stamp gstr7_last_updated explicitly on every status update
        gstDetails.setGstr7LastUpdated(LocalDateTime.now());
        
        return gstDetailsRepository.save(gstDetails);
    }

    @Transactional(readOnly = true)
    public List<PanGstr7DataResponse> fetchAllPanGstr7Data() {
        List<GstDetailsEntity> allGstDetails = gstDetailsRepository.findAll();
        
        Map<String, List<GstDetailsEntity>> groupedByPan = allGstDetails.stream()
                .filter(g -> g.getPanNumber() != null && !g.getPanNumber().trim().isEmpty())
                .collect(Collectors.groupingBy(GstDetailsEntity::getPanNumber));

        // Fetch all master HSN codes to check applicability on the fly
        java.util.Set<String> masterHsnCodes = gstr7HsnMasterRepository.findAll().stream()
                .map(com.company.grc.entity.Gstr7HsnMasterEntity::getHsnCode)
                .collect(Collectors.toSet());
                
        return groupedByPan.entrySet().stream()
                .map(entry -> {
                    String pan = entry.getKey();
                    
                    // Fetch existing HSN config for this PAN, if any
                    PanHsnConfigEntity config = panHsnConfigRepository.findById(pan).orElse(null);
                    String hsnCode = config != null ? config.getHsnCode() : null;
                    // Check applicability against both the saved flag and the current master list
                    boolean isApplicable = hsnCode != null && masterHsnCodes.contains(hsnCode);

                    List<PanGstr7DataResponse.GstinData> gstinDataList = entry.getValue().stream()
                            .map(g -> PanGstr7DataResponse.GstinData.builder()
                                    .gstin(g.getGstin())
                                    .gstr7Status(g.getGstr7Status())
                                    .gstr7DelayCount(g.getGstr7DelayCount())
                                    .gstr7LastUpdated(g.getGstr7LastUpdated())
                                    .gstType(g.getGstType())
                                    .gstdNo(g.getGstdNo())
                                    .build())
                            .collect(Collectors.toList());
                            
                    return PanGstr7DataResponse.builder()
                            .panNumber(pan)
                            .hsnCode(hsnCode)
                            .isApplicable(isApplicable)
                            .gstins(gstinDataList)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
