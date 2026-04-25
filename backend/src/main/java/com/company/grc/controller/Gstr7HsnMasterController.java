package com.company.grc.controller;

import com.company.grc.entity.Gstr7HsnMasterEntity;
import com.company.grc.repository.Gstr7HsnMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grc/admin/gstr7/hsn-master")
@RequiredArgsConstructor
public class Gstr7HsnMasterController {

    private final Gstr7HsnMasterRepository repository;

    @GetMapping
    public List<Gstr7HsnMasterEntity> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public Gstr7HsnMasterEntity add(@RequestBody Gstr7HsnMasterEntity entity) {
        return repository.save(entity);
    }

    @DeleteMapping("/{hsnCode}")
    public ResponseEntity<?> delete(@PathVariable String hsnCode) {
        repository.deleteById(hsnCode);
        return ResponseEntity.ok().build();
    }
}
