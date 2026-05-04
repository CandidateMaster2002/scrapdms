package com.company.grc.repository;

import com.company.grc.entity.Gstr7FilingDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Gstr7FilingDetailRepository extends JpaRepository<Gstr7FilingDetailEntity, Long> {

    List<Gstr7FilingDetailEntity> findByGstinOrderByReturnPeriodDesc(String gstin);

    Optional<Gstr7FilingDetailEntity> findByGstinAndReturnPeriod(String gstin, String returnPeriod);

    long countByGstinAndStatus(String gstin, String status);
    long countByGstin(String gstin);
    void deleteByGstin(String gstin);
}
