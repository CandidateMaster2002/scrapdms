package com.company.grc.repository;

import com.company.grc.entity.GstReturnsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GstReturnsRepository extends JpaRepository<GstReturnsEntity, Long> {
}
