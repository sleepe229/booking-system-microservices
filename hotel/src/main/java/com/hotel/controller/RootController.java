package com.hotel.controller;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api")
public class RootController {

    @GetMapping
    public RepresentationModel<?> getRoot() {
        RepresentationModel<?> rootModel = new RepresentationModel<>();

        rootModel.add(
                linkTo(methodOn(HotelController.class).searchHotels(null)).withRel("hotels"),
                linkTo(HotelController.class).slash("bookings").withRel("bookings")
        );

        rootModel.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(RootController.class).getRoot())
                .withSelfRel());
        rootModel.add(org.springframework.hateoas.Link.of("/swagger-ui.html").withRel("documentation"));

        return rootModel;
    }
}