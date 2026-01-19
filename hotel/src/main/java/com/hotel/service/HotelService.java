package com.hotel.service;

import com.hotel.api.dto.*;
import com.hotel.api.exception.ResourceNotFoundException;
import com.hotel.config.RabbitMQConfig;
import com.hotel.entity.Booking;
import com.hotel.entity.Hotel;
import com.hotel.events.BookingCancelledEvent;
import com.hotel.events.BookingCreatedEvent;
import com.hotel.events.BookingPaidEvent;
import com.hotel.repo.BookingRepository;
import com.hotel.repo.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

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

    public List<String> searchCities(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Возвращаем все уникальные города
            log.debug("Getting all cities");
            return hotelRepository.findAllDistinctCities();
        }

        log.debug("Searching cities with prefix: {}", query);
        return hotelRepository.findCitiesByPrefix(query.trim());
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", request.hotelId()));

        LocalDate checkIn = LocalDate.parse(request.checkIn());
        LocalDate checkOut = LocalDate.parse(request.checkOut());
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out должен быть позже check-in");
        }

        double basePrice = nights * request.guests() * hotel.getPricePerNight();

        log.info("Рассчитана цена: {} ночей × {} гостей × {} = {}",
                nights, request.guests(), hotel.getPricePerNight(), basePrice);

        String bookingId = UUID.randomUUID().toString();
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setHotelId(request.hotelId());
        booking.setStatus("PENDING");
        booking.setCustomerName(request.customerName());
        booking.setCustomerEmail(request.customerEmail());
        booking.setCheckIn(LocalDate.parse(request.checkIn()));
        booking.setCheckOut(LocalDate.parse(request.checkOut()));
        booking.setGuests(request.guests());

        Booking saved = bookingRepository.save(booking);

        BookingCreatedEvent event = new BookingCreatedEvent(
                bookingId,
                request.userId(),
                request.hotelId(),
                request.customerName(),
                request.customerEmail(),
                request.checkIn(),
                request.checkOut(),
                nights,
                hotel.getPricePerNight(),
                basePrice
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

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_BOOKING_CANCELLED,
                    event
            );
        } catch (Exception e) {
            log.error("Ошибка при публикации BookingCancelledEvent для booking_id: {}", request.bookingId(), e);
        }

        return new StatusResponse("success", null);
    }

    public BookingResponse getBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        return toResponse(booking);
    }

    public Page<BookingResponse> listBookings(int page, int size) {
        Page<Booking> bookingsPage = bookingRepository.findAll(PageRequest.of(page, size));
        return bookingsPage.map(this::toResponse);
    }

    @Transactional
    public BookingResponse payBooking(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.bookingId()));

        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new IllegalArgumentException("Можно оплатить только подтвержденные бронирования");
        }

        if (booking.getFinalPrice() == null || booking.getFinalPrice() <= 0) {
            throw new IllegalArgumentException("Цена бронирования не установлена");
        }

        booking.setStatus("PAID");
        Booking saved = bookingRepository.save(booking);

        log.info("Бронирование {} оплачено. Метод: {}, Сумма: {}",
                request.bookingId(), request.paymentMethod(), booking.getFinalPrice());

        BookingPaidEvent event = new BookingPaidEvent(
                booking.getBookingId(),
                booking.getCustomerEmail(),
                booking.getCustomerName(),
                booking.getFinalPrice(),
                request.paymentMethod(),
                System.currentTimeMillis()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_BOOKING_PAID,
                    event
            );
            log.info("BookingPaidEvent отправлен для booking_id: {}", booking.getBookingId());
        } catch (Exception e) {
            log.error("Ошибка при публикации BookingPaidEvent", e);
        }

        return toResponse(saved);
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getBookingId(),
                b.getHotelId(),
                b.getStatus(),
                b.getCustomerName(),
                b.getCustomerEmail(),
                b.getCheckIn() != null ? b.getCheckIn().toString() : null,
                b.getCheckOut() != null ? b.getCheckOut().toString() : null,
                b.getGuests(),
                b.getFinalPrice() != null ? b.getFinalPrice() : 0.0,
                b.getDiscount() != null ? b.getDiscount() : 0.0
        );
    }
}
