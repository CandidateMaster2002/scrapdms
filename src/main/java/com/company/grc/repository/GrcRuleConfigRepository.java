package com.company.grc.repository;

import com.company.grc.entity.GrcRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrcRuleConfigRepository extends JpaRepository<GrcRuleConfigEntity, String> {
}
