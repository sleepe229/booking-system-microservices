package com.hotel.booking.controller;

import com.hotel.booking.service.BookingOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingOrchestratorService orchestratorService;

    public BookingController(BookingOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {

        BookingOrchestratorService.BookingRequest serviceRequest =
                new BookingOrchestratorService.BookingRequest(
                        request.bookingId(),
                        request.userId(),
                        request.hotelId(),
                        request.nights(),
                        request.basePrice(),
                        request.isLoyalCustomer()
                );

        BookingOrchestratorService.BookingResult result =
                orchestratorService.processBooking(serviceRequest);

        BookingResponse response = new BookingResponse(
                result.bookingId(),
                result.status().name(),
                result.originalPrice(),
                result.discountPercentage(),
                result.finalPrice(),
                result.message(),
                result.recommendations()
        );

        return ResponseEntity.ok(response);
    }

    public record BookingRequest(
            String bookingId,
            String userId,
            String hotelId,
            int nights,
            double basePrice,
            boolean isLoyalCustomer
    ) {}

    public record BookingResponse(
            String bookingId,
            String status,
            double originalPrice,
            double discountPercentage,
            double finalPrice,
            String message,
            java.util.List<String> recommendations  // ✅ добавлено
    ) {}
}