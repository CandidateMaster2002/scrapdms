package com.company.grc.repository;

import com.company.grc.entity.Gstr7ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Gstr7ReviewRepository extends JpaRepository<Gstr7ReviewEntity, Long> {
    List<Gstr7ReviewEntity> findByStatusOrderBySubmittedAtDesc(String status);
}
