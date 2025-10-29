package com.hotel.service;

import com.hotel.api.dto.*;
import com.hotel.api.exception.ResourceNotFoundException;
import com.hotel.config.RabbitMQConfig;
import com.hotel.events.BookingCancelledEvent;
import com.hotel.events.BookingCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class HotelService {

    private final Map<Long, BookingResponse> bookings = new LinkedHashMap<>();
    private final AtomicLong bookingIdCounter = new AtomicLong(1);
    private final RabbitTemplate rabbitTemplate;  // НОВОЕ ПОЛЕ

    public HotelService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        return List.of(
                new HotelSearchResponse(1L, "Grand Hotel", request.city(), "Main Street 1", 150.0, true),
                new HotelSearchResponse(2L, "Budget Inn", request.city(), "Side Street 5", 80.0, true)
        );
    }

    public BookingResponse createBooking(BookingRequest request) {
        long bookingId = bookingIdCounter.getAndIncrement();
        BookingResponse booking = new BookingResponse(
                bookingId,
                request.hotelId(),
                "CREATED",
                request.customerName(),
                request.customerEmail()
        );
        bookings.put(bookingId, booking);

        BookingCreatedEvent event = new BookingCreatedEvent(
                bookingId,
                request.hotelId(),
                request.customerName(),
                request.customerEmail(),
                request.checkIn(),
                request.checkOut()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_BOOKING_CREATED,
                event
        );

        return booking;
    }

    public StatusResponse cancelBooking(CancelBookingRequest request) {
        BookingResponse existing = bookings.get(request.bookingId());
        if (existing == null) {
            throw new ResourceNotFoundException("Booking", request.bookingId());
        }
        BookingResponse cancelled = new BookingResponse(
                existing.getBookingId(),
                existing.getHotelId(),
                "CANCELLED",
                existing.getCustomerName(),
                existing.getCustomerEmail()
        );
        bookings.put(request.bookingId(), cancelled);

        BookingCancelledEvent event = new BookingCancelledEvent(
                request.bookingId(),
                existing.getCustomerEmail()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_BOOKING_CANCELLED,
                event
        );

        return new StatusResponse("success", null);
    }

    public BookingResponse getBooking(Long id) {
        BookingResponse booking = bookings.get(id);
        if (booking == null) {
            throw new ResourceNotFoundException("Booking", id);
        }
        return booking;
    }

    public Page<BookingResponse> listBookings(int page, int size) {
        List<BookingResponse> all = new ArrayList<>(bookings.values())
                .stream()
                .sorted(Comparator.comparing(BookingResponse::getBookingId))
                .collect(Collectors.toList());
        int start = Math.min(page * size, all.size());
        int end = Math.min(start + size, all.size());
        List<BookingResponse> content = all.subList(start, end);
        return new PageImpl<>(content, PageRequest.of(page, size), all.size());
    }
}