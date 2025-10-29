package com.hotel.assemblers;

import com.hotel.api.dto.HotelSearchResponse;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class HotelModelAssembler implements RepresentationModelAssembler<HotelSearchResponse, EntityModel<HotelSearchResponse>> {

    @Override
    public EntityModel<HotelSearchResponse> toModel(HotelSearchResponse hotel) {
        return EntityModel.of(hotel,
                linkTo(methodOn(com.hotel.controller.HotelController.class).searchHotels(new com.hotel.api.dto.HotelSearchRequest(hotel.city(), "1970-01-01", "1970-01-01", 1))).withSelfRel()
        );
    }
}
