package com.company.grc.repository;

import com.company.grc.entity.GstDetailsEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GstDetailsRepository extends JpaRepository<GstDetailsEntity, String> {

    @Query("SELECT g.gstin FROM GstDetailsEntity g")
    List<String> findAllGstins();

    List<GstDetailsEntity> findByApiErrorFalseOrApiErrorIsNull();

    @Query("SELECT g FROM GstDetailsEntity g WHERE g.createdAt IS NOT NULL ORDER BY g.createdAt DESC")
    List<GstDetailsEntity> findNewVendors(Pageable pageable);
}
