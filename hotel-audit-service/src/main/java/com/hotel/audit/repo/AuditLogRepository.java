package com.hotel.audit.repo;

import com.hotel.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByBookingId(Long bookingId);

    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    Page<AuditLog> findByBookingId(Long bookingId, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT a FROM AuditLog a WHERE a.customerEmail = :email ORDER BY a.timestamp DESC")
    List<AuditLog> findByCustomerEmail(@Param("email") String email);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType")
    List<Object[]> countByEventType();

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentEvents(@Param("since") LocalDateTime since);

    @Query(value = "SELECT * FROM audit_log WHERE event_data::text LIKE %:searchTerm% ORDER BY timestamp DESC",
            nativeQuery = true)
    List<AuditLog> searchInEventData(@Param("searchTerm") String searchTerm);
}