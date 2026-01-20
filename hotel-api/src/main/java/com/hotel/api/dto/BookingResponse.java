package com.hotel.api.dto;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import java.util.Objects;

@Relation(collectionRelation = "bookings", itemRelation = "booking")
public class BookingResponse extends RepresentationModel<BookingResponse> {

    private final String bookingId;
    private final String hotelId;
    private final String status;
    private final String customerName;
    private final String customerEmail;

    private final String checkIn;
    private final String checkOut;
    private final Integer guests;

    private final Double finalPrice;
    private final Double discount;

    public BookingResponse(
            String bookingId,
            String hotelId,
            String status,
            String customerName,
            String customerEmail,
            String checkIn,
            String checkOut,
            Integer guests,
            Double finalPrice,
            Double discount
    ) {
        this.bookingId = bookingId;
        this.hotelId = hotelId;
        this.status = status;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.finalPrice = finalPrice;
        this.discount = discount;
    }

    public String getBookingId() { return bookingId; }
    public String getHotelId() { return hotelId; }
    public String getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public Integer getGuests() { return guests; }
    public Double getFinalPrice() { return finalPrice; }
    public Double getDiscount() { return discount; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BookingResponse that = (BookingResponse) o;
        return Objects.equals(getBookingId(), that.getBookingId()) && Objects.equals(getHotelId(), that.getHotelId()) && Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getCustomerName(), that.getCustomerName()) && Objects.equals(getCustomerEmail(), that.getCustomerEmail()) && Objects.equals(getCheckIn(), that.getCheckIn()) && Objects.equals(getCheckOut(), that.getCheckOut()) && Objects.equals(getGuests(), that.getGuests()) && Objects.equals(getFinalPrice(), that.getFinalPrice()) && Objects.equals(getDiscount(), that.getDiscount());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBookingId(), getHotelId(), getStatus(), getCustomerName(), getCustomerEmail(), getCheckIn(), getCheckOut(), getGuests(), getFinalPrice(), getDiscount());
    }
}
