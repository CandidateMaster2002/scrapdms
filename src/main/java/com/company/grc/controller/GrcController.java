package com.company.grc.controller;

import com.company.grc.dto.ApiDto;
import com.company.grc.service.GrcCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grc")
public class GrcController {

    private final GrcCalculationService grcCalculationService;

    @Autowired
    public GrcController(GrcCalculationService grcCalculationService) {
        this.grcCalculationService = grcCalculationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiDto.GrcResponse> calculateScore(@RequestBody ApiDto.GrcRequest request) {
        ApiDto.GrcResponse response = grcCalculationService.calculateScore(request.getGstin());
        return ResponseEntity.ok(response);
    }
}
