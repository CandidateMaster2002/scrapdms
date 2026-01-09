package com.company.grc.repository;

import com.company.grc.entity.GrcScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrcScoreRepository extends JpaRepository<GrcScoreEntity, Long> {
    Optional<GrcScoreEntity> findTopByGstinOrderByCalculatedAtDesc(String gstin);
}
