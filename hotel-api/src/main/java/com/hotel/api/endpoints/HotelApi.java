package com.hotel.api.endpoints;

import com.hotel.api.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "gateway", description = "API для поиска и бронирования отелей (HAL)")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = com.hotel.api.dto.StatusResponse.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = com.hotel.api.dto.StatusResponse.class)))
})
public interface HotelApi {

    @Operation(summary = "Поиск доступных отелей")
    @PostMapping(value = "/api/hotels/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    CollectionModel<EntityModel<com.hotel.api.dto.HotelSearchResponse>> searchHotels(@Valid @RequestBody com.hotel.api.dto.HotelSearchRequest request);

    @Operation(summary = "Создать бронирование")
    @PostMapping(value = "/api/bookings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<com.hotel.api.dto.BookingResponse>> createBooking(@Valid @RequestBody com.hotel.api.dto.BookingRequest request);

    @Operation(summary = "Отменить бронирование")
    @PostMapping(value = "/api/bookings/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<com.hotel.api.dto.StatusResponse> cancelBooking(@Valid @RequestBody com.hotel.api.dto.CancelBookingRequest request);

    @Operation(summary = "Получить бронирование по ID")
    @GetMapping("/api/bookings/{id}")
    EntityModel<com.hotel.api.dto.BookingResponse> getBooking(@PathVariable("id") String id);

    @Operation(summary = "Список бронирований (пагинация)")
    @GetMapping("/api/bookings")
    PagedModel<EntityModel<com.hotel.api.dto.BookingResponse>> listBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
}
