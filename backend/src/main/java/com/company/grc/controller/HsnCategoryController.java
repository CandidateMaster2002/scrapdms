package com.company.grc.controller;

import com.company.grc.entity.HsnCategoryEntity;
import com.company.grc.entity.Gstr7HsnMasterEntity;
import com.company.grc.repository.HsnCategoryRepository;
import com.company.grc.repository.Gstr7HsnMasterRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grc/admin/gstr7/hsn-categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HsnCategoryController {

    private final HsnCategoryRepository categoryRepository;
    private final Gstr7HsnMasterRepository hsnMasterRepository;

    private boolean isNotAdmin(String role) {
        return !"super_admin".equals(role);
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Role", required = false) String role,
            @RequestBody CategoryRequest req) {
        if (isNotAdmin(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        HsnCategoryEntity entity = HsnCategoryEntity.builder()
                .name(req.getName())
                .description(req.getDescription())
                .build();
        return ResponseEntity.ok(categoryRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/codes")
    public ResponseEntity<?> addCode(
            @PathVariable Long id,
            @RequestHeader(value = "Role", required = false) String role,
            @RequestBody CodeRequest req) {
        if (isNotAdmin(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        HsnCategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        Gstr7HsnMasterEntity code = Gstr7HsnMasterEntity.builder()
                .hsnCode(req.getHsnCode().trim())
                .description(req.getDescription())
                .category(category)
                .build();
        hsnMasterRepository.save(code);
        return ResponseEntity.ok(categoryRepository.findById(id).orElseThrow());
    }

    @DeleteMapping("/{id}/codes/{hsnCode}")
    public ResponseEntity<?> removeCode(
            @PathVariable Long id,
            @PathVariable String hsnCode,
            @RequestHeader(value = "Role", required = false) String role) {
        if (isNotAdmin(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        hsnMasterRepository.deleteById(hsnCode);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CategoryRequest {
        private String name;
        private String description;
    }

    @Data
    public static class CodeRequest {
        private String hsnCode;
        private String description;
    }
}
