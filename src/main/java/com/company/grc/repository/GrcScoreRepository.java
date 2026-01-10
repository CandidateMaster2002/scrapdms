package com.company.grc.repository;

import com.company.grc.entity.GrcScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrcScoreRepository extends JpaRepository<GrcScoreEntity, String> {
    // findById(gstin) is now sufficient to find the single score record.
}
