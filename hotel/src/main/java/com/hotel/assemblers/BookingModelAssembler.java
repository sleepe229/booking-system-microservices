package com.hotel.assemblers;

import com.hotel.api.dto.BookingResponse;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class BookingModelAssembler implements RepresentationModelAssembler<BookingResponse, EntityModel<BookingResponse>> {

    @Override
    public EntityModel<BookingResponse> toModel(BookingResponse booking) {
        return EntityModel.of(booking,
                linkTo(methodOn(com.hotel.controller.HotelController.class).getBooking(booking.getBookingId())).withSelfRel(),
                linkTo(methodOn(com.hotel.controller.HotelController.class).cancelBooking(new com.hotel.api.dto.CancelBookingRequest(booking.getBookingId()))).withRel("cancel"),
                linkTo(methodOn(com.hotel.controller.HotelController.class).listBookings(0, 10)).withRel("collection")
        );
    }
}
