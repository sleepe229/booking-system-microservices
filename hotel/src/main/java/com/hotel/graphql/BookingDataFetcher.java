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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

@DgsComponent
public class BookingDataFetcher {

    private final HotelService hotelService;

    @Autowired
    public BookingDataFetcher(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @DgsQuery
    public BookingResponse bookingById(@InputArgument("id") Long id) {
        return hotelService.getBooking(id);
    }

    @DgsQuery
    public Map<String, Object> bookings(@InputArgument("page") int page, @InputArgument("size") int size) {
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
        BookingRequest request = new BookingRequest(
                ((Number) input.get("hotelId")).longValue(),
                (String) input.get("checkIn"),
                (String) input.get("checkOut"),
                (Integer) input.get("guests"),
                (String) input.get("customerName"),
                (String) input.get("customerEmail")
        );
        return hotelService.createBooking(request);
    }

    @DgsMutation
    public StatusResponse cancelBooking(@InputArgument("input") Map<String, Object> input) {
        CancelBookingRequest request = new CancelBookingRequest(
                ((Number) input.get("bookingId")).longValue()
        );
        return hotelService.cancelBooking(request);
    }

    @DgsMutation
    public BookingResponse updateBookingRoomType(@InputArgument("id") Long id,
                                                 @InputArgument("roomType") String roomType) {
        BookingResponse existing = hotelService.getBooking(id);
        return existing;
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
}