package com.company.grc.repository;

import com.company.grc.entity.GstDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GstDetailsRepository extends JpaRepository<GstDetailsEntity, String> {

    @org.springframework.data.jpa.repository.Query("SELECT g.gstin FROM GstDetailsEntity g")
    java.util.List<String> findAllGstins();
}
