package com.hotel.controller;

import com.hotel.assemblers.BookingModelAssembler;
import com.hotel.assemblers.HotelModelAssembler;
import com.hotel.api.endpoints.HotelApi;
import com.hotel.api.dto.*;
import com.hotel.service.HotelService;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class HotelController implements HotelApi {

    private final HotelService hotelService;
    private final BookingModelAssembler bookingAssembler;
    private final HotelModelAssembler hotelAssembler;
    private final PagedResourcesAssembler<BookingResponse> pagedResourcesAssembler;

    public HotelController(HotelService hotelService,
                           BookingModelAssembler bookingAssembler,
                           HotelModelAssembler hotelAssembler,
                           PagedResourcesAssembler<BookingResponse> pagedResourcesAssembler) {
        this.hotelService = hotelService;
        this.bookingAssembler = bookingAssembler;
        this.hotelAssembler = hotelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public CollectionModel<EntityModel<HotelSearchResponse>> searchHotels(HotelSearchRequest request) {
        List<HotelSearchResponse> list = hotelService.searchHotels(request);
        var models = list.stream().map(hotelAssembler::toModel).collect(Collectors.toList());
        return CollectionModel.of(models, Link.of("/api/hotels/search").withSelfRel());
    }

    @Override
    public ResponseEntity<EntityModel<BookingResponse>> createBooking(BookingRequest request) {
        BookingResponse created = hotelService.createBooking(request);
        EntityModel<BookingResponse> model = bookingAssembler.toModel(created);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    @Override
    public EntityModel<StatusResponse> cancelBooking(CancelBookingRequest request) {
        StatusResponse res = hotelService.cancelBooking(request);
        return EntityModel.of(res, Link.of("/api/bookings/cancel").withSelfRel());
    }

    @Override
    public EntityModel<BookingResponse> getBooking(Long id) {
        BookingResponse booking = hotelService.getBooking(id);
        return bookingAssembler.toModel(booking);
    }

    @Override
    public PagedModel<EntityModel<BookingResponse>> listBookings(int page, int size) {
        Page<BookingResponse> pageResult = hotelService.listBookings(page, size);
        return pagedResourcesAssembler.toModel(pageResult, bookingAssembler);
    }
}
