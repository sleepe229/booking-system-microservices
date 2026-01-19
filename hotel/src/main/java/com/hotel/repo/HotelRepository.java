package com.hotel.repo;

import com.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, String> {
    List<Hotel> findByCityAndAvailableTrue(String city);

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.available = true ORDER BY h.city")
    List<String> findAllDistinctCities();

    @Query("SELECT DISTINCT h.city FROM Hotel h WHERE h.available = true AND LOWER(h.city) LIKE LOWER(CONCAT(:query, '%')) ORDER BY h.city")
    List<String> findCitiesByPrefix(@Param("query") String query);
}
