package com.hotel.service;

import com.hotel.api.dto.*;
import com.hotel.api.exception.ResourceNotFoundException;
import com.hotel.config.RabbitMQConfig;
import com.hotel.entity.Booking;
import com.hotel.entity.Hotel;
import com.hotel.events.BookingCancelledEvent;
import com.hotel.events.BookingCreatedEvent;
import com.hotel.repo.BookingRepository;
import com.hotel.repo.HotelRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HotelService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RabbitTemplate rabbitTemplate;

    public HotelService(BookingRepository bookingRepository,
                        HotelRepository hotelRepository,
                        RabbitTemplate rabbitTemplate) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        List<Hotel> hotels = hotelRepository.findByCityAndAvailableTrue(request.city());
        return hotels.stream()
                .map(h -> new HotelSearchResponse(
                        h.getHotelId(),
                        h.getName(),
                        h.getCity(),
                        h.getAddress(),
                        h.getPricePerNight(),
                        h.getAvailable()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Booking booking = new Booking();
        booking.setHotelId(request.hotelId());
        booking.setStatus("CREATED");
        booking.setCustomerName(request.customerName());
        booking.setCustomerEmail(request.customerEmail());
        booking.setCheckIn(LocalDate.parse(request.checkIn()));
        booking.setCheckOut(LocalDate.parse(request.checkOut()));
        booking.setGuests(request.guests());

        Booking saved = bookingRepository.save(booking);

        BookingCreatedEvent event = new BookingCreatedEvent(
                saved.getBookingId(),
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

        return toResponse(saved);
    }

    @Transactional
    public StatusResponse cancelBooking(CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.bookingId()));

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        BookingCancelledEvent event = new BookingCancelledEvent(
                request.bookingId(),
                booking.getCustomerEmail()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY_BOOKING_CANCELLED,
                event
        );

        return new StatusResponse("success", null);
    }

    public BookingResponse getBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        return toResponse(booking);
    }

    public Page<BookingResponse> listBookings(int page, int size) {
        Page<Booking> bookingsPage = bookingRepository.findAll(PageRequest.of(page, size));
        return bookingsPage.map(this::toResponse);
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getBookingId(),
                b.getHotelId(),
                b.getStatus(),
                b.getCustomerName(),
                b.getCustomerEmail()
        );
    }
}