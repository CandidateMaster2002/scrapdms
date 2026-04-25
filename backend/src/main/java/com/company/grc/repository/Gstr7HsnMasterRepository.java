package com.company.grc.repository;

import com.company.grc.entity.Gstr7HsnMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Gstr7HsnMasterRepository extends JpaRepository<Gstr7HsnMasterEntity, String> {
}
