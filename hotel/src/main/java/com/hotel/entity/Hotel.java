package com.hotel.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hotels")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hotelId;

    private String name;
    private String city;
    private String address;
    private Double pricePerNight;
    private Boolean available;

    public Hotel() {}

    public Hotel(Long hotelId, String name, String city, String address, Double pricePerNight, Boolean available) {
        this.hotelId = hotelId;
        this.name = name;
        this.city = city;
        this.address = address;
        this.pricePerNight = pricePerNight;
        this.available = available;
    }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}