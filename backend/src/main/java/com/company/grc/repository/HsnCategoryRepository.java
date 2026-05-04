package com.company.grc.repository;

import com.company.grc.entity.HsnCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HsnCategoryRepository extends JpaRepository<HsnCategoryEntity, Long> {
}
