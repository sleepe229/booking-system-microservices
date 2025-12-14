package com.hotel.repo;

import com.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, String> {
    List<Hotel> findByCityAndAvailableTrue(String city);
}