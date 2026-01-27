package com.company.mdm.repository;

import com.company.mdm.domain.entity.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Command Repository - Simplified
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

        /**
         * Get latest command for a device by command type
         * Returns the most recent command (latest by created_at)
         */
        @Query("SELECT c FROM Command c WHERE c.deviceId = :deviceId " +
                        "AND c.commandType = :commandType " +
                        "ORDER BY c.createdAt DESC LIMIT 1")
        Optional<Command> findLatestByDeviceIdAndCommandType(
                        @Param("deviceId") Long deviceId,
                        @Param("commandType") String commandType);

        /**
         * Get all commands for a device
         */
        List<Command> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

        /**
         * Get all commands by type
         */
        List<Command> findByCommandType(String commandType);

        /**
         * Delete old commands for a device by command type (keep only latest)
         */
        @Query("DELETE FROM Command c WHERE c.deviceId = :deviceId " +
                        "AND c.commandType = :commandType " +
                        "AND c.id NOT IN (" +
                        "   SELECT c2.id FROM Command c2 " +
                        "   WHERE c2.deviceId = :deviceId " +
                        "   AND c2.commandType = :commandType " +
                        "   ORDER BY c2.createdAt DESC LIMIT 1" +
                        ")")
        void deleteOldCommandsByDeviceAndType(
                        @Param("deviceId") Long deviceId,
                        @Param("commandType") String commandType);
}
