package com.company.mdm.repository;

import com.company.mdm.domain.entity.Command;
import com.company.mdm.domain.entity.CommandStatus;
import com.company.mdm.domain.entity.CommandType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Command Repository
 * 
 * Data access for Command entities
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    /**
     * Find all pending commands for a device
     * Used by /status endpoint to deliver commands
     */
    @Query("SELECT c FROM Command c WHERE c.deviceId = :deviceId " +
            "AND c.status = 'PENDING' " +
            "AND (c.expiresAt IS NULL OR c.expiresAt > :now) " +
            "ORDER BY c.priority DESC, c.createdAt ASC")
    List<Command> findPendingCommandsForDevice(
            @Param("deviceId") Long deviceId,
            @Param("now") LocalDateTime now);

    /**
     * Find all commands for a device
     */
    List<Command> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    /**
     * Find commands by status
     */
    List<Command> findByStatus(CommandStatus status);

    /**
     * Find commands by type
     */
    List<Command> findByCommandType(CommandType commandType);

    /**
     * Find expired commands
     */
    @Query("SELECT c FROM Command c WHERE c.status = 'PENDING' " +
            "AND c.expiresAt IS NOT NULL " +
            "AND c.expiresAt < :now")
    List<Command> findExpiredCommands(@Param("now") LocalDateTime now);

    /**
     * Count pending commands for device
     */
    @Query("SELECT COUNT(c) FROM Command c WHERE c.deviceId = :deviceId " +
            "AND c.status = 'PENDING'")
    Long countPendingCommandsForDevice(@Param("deviceId") Long deviceId);

    /**
     * Find commands by device and status
     */
    List<Command> findByDeviceIdAndStatus(Long deviceId, CommandStatus status);
}
