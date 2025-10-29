package com.hotel.api.dto;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import java.util.Objects;

@Relation(collectionRelation = "bookings", itemRelation = "booking")
public class BookingResponse extends RepresentationModel<BookingResponse> {

    private final Long bookingId;
    private final Long hotelId;
    private final String status;
    private final String customerName;
    private final String customerEmail;

    public BookingResponse(Long bookingId, Long hotelId, String status, String customerName, String customerEmail) {
        this.bookingId = bookingId;
        this.hotelId = hotelId;
        this.status = status;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    public Long getBookingId() { return bookingId; }
    public Long getHotelId() { return hotelId; }
    public String getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }

    @Override
    public boolean equals(Object o) { /* standard equals including super.equals */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BookingResponse that = (BookingResponse) o;
        return Objects.equals(bookingId, that.bookingId) &&
                Objects.equals(hotelId, that.hotelId) &&
                Objects.equals(status, that.status) &&
                Objects.equals(customerName, that.customerName) &&
                Objects.equals(customerEmail, that.customerEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bookingId, hotelId, status, customerName, customerEmail);
    }
}
