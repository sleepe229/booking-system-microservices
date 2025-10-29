package com.hotel.audit.controller;

import com.hotel.audit.entity.AuditLog;
import com.hotel.audit.repo.AuditLogRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCSV(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String eventType
    ) {
        List<AuditLog> logs;

        if (eventType != null && !eventType.isEmpty()) {
            logs = auditLogRepository.findByEventType(eventType);
        } else if (startDate != null && endDate != null) {
            logs = auditLogRepository.findByTimestampBetween(startDate, endDate);
        } else {
            logs = auditLogRepository.findAll();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("ID,Event Type,Timestamp,Booking ID,Customer Email,Event Data");

            for (AuditLog log : logs) {
                writer.printf("%d,%s,%s,%s,%s,\"%s\"%n",
                        log.getId(),
                        log.getEventType(),
                        log.getTimestamp(),
                        log.getBookingId() != null ? log.getBookingId() : "",
                        log.getCustomerEmail() != null ? log.getCustomerEmail() : "",
                        log.getEventData().replace("\"", "\"\"")
                );
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "audit_log_" + System.currentTimeMillis() + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(baos.toByteArray());
    }

    @GetMapping("/logs")
    public List<AuditLog> getLogs(
            @RequestParam(required = false) Long bookingId,
            @RequestParam(required = false) String eventType
    ) {
        if (bookingId != null) {
            return auditLogRepository.findByBookingId(bookingId);
        }
        if (eventType != null) {
            return auditLogRepository.findByEventType(eventType);
        }
        return auditLogRepository.findAll();
    }
}