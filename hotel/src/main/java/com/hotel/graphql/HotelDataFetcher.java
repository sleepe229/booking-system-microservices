package com.hotel.graphql;

import com.hotel.api.dto.HotelSearchRequest;
import com.hotel.api.dto.HotelSearchResponse;
import com.hotel.service.HotelService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@DgsComponent
public class HotelDataFetcher {

    private final HotelService hotelService;

    @Autowired
    public HotelDataFetcher(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @DgsQuery
    public List<HotelSearchResponse> searchHotels(@InputArgument("input") Map<String, Object> input,
                                                  @InputArgument("roomTypeFilter") String roomTypeFilter) {
        HotelSearchRequest request = new HotelSearchRequest(
                (String) input.get("city"),
                (String) input.get("checkIn"),
                (String) input.get("checkOut"),
                (Integer) input.get("guests")
        );

        List<HotelSearchResponse> hotels = hotelService.searchHotels(request);

        // Если указан фильтр по типу комнаты, можно добавить логику фильтрации
        // Пока просто возвращаем все отели
        return hotels;
    }
}