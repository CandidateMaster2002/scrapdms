package com.company.grc.controller;

import com.company.grc.dto.PanGstr7DataResponse;
import com.company.grc.entity.PanHsnConfigEntity;
import com.company.grc.entity.GstDetailsEntity;
import com.company.grc.service.Gstr7Service;
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
public class Gstr7AdminController {

    private final Gstr7Service gstr7Service;

    private boolean isNotAdmin(String role) {
        return !"super_admin".equals(role);
    }

    @GetMapping("/pans")
    public ResponseEntity<?> getAllPanGstr7Data(
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. super_admin role required.");
        }
        List<PanGstr7DataResponse> data = gstr7Service.fetchAllPanGstr7Data();
        return ResponseEntity.ok(data);
    }

    @PostMapping("/hsn")
    public ResponseEntity<?> saveOrUpdateHsn(
            @RequestHeader(value = "Role", required = false) String role,
            @RequestBody HsnRequest request) {
        if (isNotAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. super_admin role required.");
        }
        PanHsnConfigEntity config = gstr7Service.saveOrUpdateHsn(
                request.getPan(), request.getCategoryId(), request.getUpdatedBy());
        return ResponseEntity.ok(config);
    }

    @PutMapping("/gstd/{gstin}")
    public ResponseEntity<?> markUnmarkGstd(
            @PathVariable String gstin,
            @RequestParam(required = false) String gstdNo,
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. super_admin role required.");
        }
        GstDetailsEntity updated = gstr7Service.markGstinAsGstd(gstin, gstdNo);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/status/{gstin}")
    public ResponseEntity<?> updateGstr7Status(
            @PathVariable String gstin,
            @RequestBody Gstr7StatusRequest request,
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied. super_admin role required.");
        }
        GstDetailsEntity updated = gstr7Service.updateGstr7Data(
                gstin, request.getStatus(), request.getDelayCount(), request.getMissedCount());
        return ResponseEntity.ok(updated);
    }

    @Data
    public static class HsnRequest {
        private String pan;
        private Long categoryId;
        private String updatedBy;
    }

    @Data
    public static class Gstr7StatusRequest {
        private String status;
        private Integer delayCount;
        private Integer missedCount;
    }
}
