package com.company.mdm.repository;

import com.company.mdm.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Device Repository - Simplified
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

       /**
        * Find active device by fulluuid
        * CRITICAL: Only returns device if status = TRUE
        */
       @Query("SELECT d FROM Device d WHERE d.fulluuid = :fulluuid AND d.status = TRUE")
       Optional<Device> findByFulluuidAndStatusTrue(@Param("fulluuid") String fulluuid);

       /**
        * Find device by fulluuid (regardless of status)
        * Used for validation during device creation
        */
       Optional<Device> findByFulluuid(String fulluuid);

       /**
        * Find active device by pushToken hash
        * Used for device authentication
        */
       @Query("SELECT d FROM Device d WHERE d.tokenHash = :tokenHash AND d.status = TRUE")
       Optional<Device> findByTokenHashAndStatusTrue(@Param("tokenHash") String tokenHash);

       /**
        * Check if any active device exists with this fulluuid
        */
       @Query("SELECT COUNT(d) > 0 FROM Device d WHERE d.fulluuid = :fulluuid AND d.status = TRUE")
       boolean existsByFulluuidAndStatusTrue(@Param("fulluuid") String fulluuid);
}
