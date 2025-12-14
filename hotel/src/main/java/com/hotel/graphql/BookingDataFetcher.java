package com.hotel.graphql;

import com.hotel.api.dto.BookingRequest;
import com.hotel.api.dto.BookingResponse;
import com.hotel.api.dto.CancelBookingRequest;
import com.hotel.api.dto.StatusResponse;
import com.hotel.service.HotelService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

@DgsComponent
public class BookingDataFetcher {

    private static final Logger log = LoggerFactory.getLogger(BookingDataFetcher.class);

    private final HotelService hotelService;

    @Autowired
    public BookingDataFetcher(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @DgsQuery
    public BookingResponse bookingById(@InputArgument("id") String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("bookingId не может быть пустым");
        }
        return hotelService.getBooking(id);
    }

    @DgsQuery
    public Map<String, Object> bookings(@InputArgument("page") int page, @InputArgument("size") int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page не может быть отрицательной");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size должен быть в диапазоне 1-100");
        }

        Page<BookingResponse> pageResult = hotelService.listBookings(page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("content", pageResult.getContent());
        result.put("pageNumber", pageResult.getNumber());
        result.put("pageSize", pageResult.getSize());
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("last", pageResult.isLast());

        return result;
    }

    @DgsMutation
    public BookingResponse createBooking(@InputArgument("input") Map<String, Object> input) {
        try {
            String hotelId = parseString(input.get("hotelId"));

            BookingRequest request = new BookingRequest(
                    hotelId,
                    (String) input.get("checkIn"),
                    (String) input.get("checkOut"),
                    (Integer) input.get("guests"),
                    (String) input.get("customerName"),
                    (String) input.get("customerEmail"),
                    (String) input.get("userId")
            );
            return hotelService.createBooking(request);
        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    @DgsMutation
    public StatusResponse cancelBooking(@InputArgument("input") Map<String, Object> input) {
        try {
            String bookingId = parseString(input.get("bookingId"));

            if (bookingId == null || bookingId.isEmpty()) {
                throw new IllegalArgumentException("bookingId не может быть пустым");
            }

            CancelBookingRequest request = new CancelBookingRequest(bookingId);
            return hotelService.cancelBooking(request);
        } catch (Exception e) {
            log.error("Error cancelling booking: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cancel booking: " + e.getMessage(), e);
        }
    }

    @DgsMutation
    public BookingResponse updateBookingRoomType(@InputArgument("id") String id,
                                                 @InputArgument("roomType") String roomType) {
        try {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("bookingId не может быть пустым");
            }

            return hotelService.getBooking(id);
        } catch (Exception e) {
            log.error("Error updating booking room type: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update booking: " + e.getMessage(), e);
        }
    }

    @DgsData(parentType = "Booking", field = "hotel")
    public Map<String, Object> hotel(DataFetchingEnvironment dfe) {
        BookingResponse booking = dfe.getSource();

        Map<String, Object> hotel = new HashMap<>();
        hotel.put("hotelId", booking.getHotelId());
        hotel.put("name", "Hotel Name");
        hotel.put("city", "City");

        return hotel;
    }

    private String parseString(Object value) {
        return switch (value) {
            case null -> null;
            case String s -> s;
            default -> value.toString();
        };
    }

}