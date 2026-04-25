package com.company.grc.repository;

import com.company.grc.entity.PanHsnConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanHsnConfigRepository extends JpaRepository<PanHsnConfigEntity, String> {
}
